package top.tasaed.aoh2de.llm.playing;

import io.javalin.Javalin;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;

public final class WebSocketServerTransport implements Transport {
    private final LPConfig config;
    private final ApiDispatcher dispatcher;
    private final Map<Session, Connection> connections = new ConcurrentHashMap<>();
    private Javalin server;
    private volatile boolean accepting;
    private volatile boolean running;

    public WebSocketServerTransport(LPConfig config, ApiDispatcher dispatcher) {
        this.config = config;
        this.dispatcher = dispatcher;
    }

    @Override
    public synchronized void start() throws IOException {
        if (server != null) {
            return;
        }
        config.validate();
        Javalin candidate = Javalin.create(javalin -> {
            javalin.router.ignoreTrailingSlashes = false;
            javalin.jetty.modifyWebSocketServletFactory(factory -> {
                // Jetty enforces these limits while assembling fragmented messages.
                factory.setMaxTextMessageSize(ApiDispatcher.MAX_REQUEST_BODY_SIZE);
                factory.setMaxBinaryMessageSize(ApiDispatcher.MAX_REQUEST_BODY_SIZE);
            });
            javalin.routes.ws(config.getWsPath(), websocket -> {
                websocket.onConnect(context -> connected(context.session));
                websocket.onMessage(context -> {
                    Connection connection = connections.get(context.session);
                    if (connection != null) {
                        connection.receive(context.message());
                    }
                });
                websocket.onBinaryMessage(context -> {
                    Connection connection = connections.get(context.session);
                    if (connection != null) {
                        connection.stopWorker();
                    }
                    context.closeSession(1003, "Binary messages are not supported.");
                });
                websocket.onClose(context -> disconnected(context.session));
                websocket.onError(context -> disconnected(context.session));
            });
        });
        accepting = true;
        try {
            candidate.start(config.getHost(), config.getPort());
            server = candidate;
            running = true;
        } catch (RuntimeException exception) {
            accepting = false;
            closeConnections();
            try {
                candidate.stop();
            } catch (RuntimeException cleanupFailure) {
                exception.addSuppressed(cleanupFailure);
            }
            throw new IOException("Could not start the WebSocket server.", exception);
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public synchronized void close() {
        accepting = false;
        running = false;
        closeConnections();
        Javalin previous = server;
        server = null;
        if (previous != null) {
            previous.stop();
        }
    }

    private void connected(Session session) {
        if (!accepting) {
            session.disconnect();
            return;
        }
        Connection connection = new Connection(session);
        connections.put(session, connection);
        // Covers a connection accepted concurrently with transport shutdown.
        if (!accepting) {
            connections.remove(session, connection);
            connection.close();
        }
    }

    private void disconnected(Session session) {
        Connection connection = connections.remove(session);
        if (connection != null) {
            connection.stopWorker();
        }
        // Jetty owns the close handshake, including protocol-error close codes.
    }

    private void closeConnections() {
        connections.values().forEach(Connection::close);
        connections.clear();
    }

    private final class Connection {
        private final Session session;
        private final ExecutorService worker = WebSocketProtocol.newWorker("lp-ws-server");
        private final AtomicBoolean closed = new AtomicBoolean();

        private Connection(Session session) {
            this.session = session;
        }

        private void receive(String message) {
            if (closed.get()) {
                return;
            }
            try {
                worker.execute(() -> dispatch(message));
            } catch (RejectedExecutionException ignored) {
                // A close callback has already shut down this connection's worker.
            }
        }

        private void dispatch(String message) {
            if (closed.get()) {
                return;
            }
            try {
                String response = WebSocketProtocol.respond(dispatcher, message);
                if (closed.get() || Thread.currentThread().isInterrupted()) {
                    return;
                }
                CompletableFuture<Void> sent = new CompletableFuture<>();
                session.sendText(response, new Callback() {
                    @Override
                    public void succeed() {
                        sent.complete(null);
                    }

                    @Override
                    public void fail(Throwable failure) {
                        sent.completeExceptionally(failure);
                    }
                });
                // Only the owned worker waits; receives and close callbacks remain live.
                sent.get();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException | RuntimeException exception) {
                close();
            }
        }

        private void stopWorker() {
            if (closed.compareAndSet(false, true)) {
                worker.shutdownNow();
            }
        }

        private void close() {
            stopWorker();
            session.disconnect();
        }
    }
}
