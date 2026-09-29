package top.tasaed.aoh2de.llm.playing;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class WebSocketClientTransport implements Transport {
    private final LPConfig config;
    private final ApiDispatcher dispatcher;
    private volatile Connection connection;

    public WebSocketClientTransport(LPConfig config, ApiDispatcher dispatcher) {
        this.config = config;
        this.dispatcher = dispatcher;
    }

    @Override
    public void start() throws IOException {
        config.validate();
        Connection candidate;
        synchronized (this) {
            if (connection != null && !connection.closed.get()) {
                return;
            }
            candidate = new Connection();
            connection = candidate;
        }
        try {
            // HttpClient I/O must not share the worker that waits for game-thread results.
            CompletableFuture<WebSocket> connecting = HttpClient.newHttpClient()
                    .newWebSocketBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .buildAsync(URI.create(config.getWsUrl()), candidate);
            candidate.connecting = connecting;
            if (candidate.closed.get()) {
                connecting.cancel(true);
            }
            WebSocket socket = connecting.get();
            if (candidate.closed.get() || socket.isInputClosed() || socket.isOutputClosed()) {
                throw new IOException("The WebSocket connection closed during startup.");
            }
        } catch (InterruptedException exception) {
            candidate.stop(true);
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while connecting the WebSocket client.", exception);
        } catch (ExecutionException | CancellationException exception) {
            candidate.stop(true);
            throw new IOException("Could not connect the WebSocket client.", exception);
        } catch (IOException | RuntimeException exception) {
            candidate.stop(true);
            if (exception instanceof IOException ioException) {
                throw ioException;
            }
            throw new IOException("Could not connect the WebSocket client.", exception);
        }
    }

    @Override
    public boolean isRunning() {
        Connection current = connection;
        if (current == null || current.closed.get()) {
            return false;
        }
        WebSocket socket = current.socket;
        return socket != null && !socket.isInputClosed() && !socket.isOutputClosed();
    }

    @Override
    public void close() {
        Connection previous;
        synchronized (this) {
            previous = connection;
            connection = null;
        }
        if (previous != null) {
            previous.stop(true);
        }
    }

    private final class Connection implements WebSocket.Listener {
        private final ExecutorService worker = WebSocketProtocol.newWorker("lp-ws-client");
        private final AtomicBoolean closed = new AtomicBoolean();
        private final WebSocketProtocol.TextAccumulator text = new WebSocketProtocol.TextAccumulator();
        private volatile WebSocket socket;
        private volatile CompletableFuture<WebSocket> connecting;

        @Override
        public void onOpen(WebSocket webSocket) {
            socket = webSocket;
            if (closed.get()) {
                webSocket.abort();
            } else {
                webSocket.request(1);
            }
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            if (closed.get()) {
                webSocket.request(1);
                return null;
            }
            if (!text.append(data)) {
                reject(webSocket, 1009, "The message exceeds 65536 UTF-8 bytes.");
            } else if (last) {
                String message = text.take();
                try {
                    worker.execute(() -> dispatch(webSocket, message));
                } catch (RejectedExecutionException ignored) {
                    // A concurrent close has already cancelled pending work.
                }
            }
            // Demand continues immediately, even while the game thread is busy, so that
            // fragmented messages, pings and remote close frames can still be received.
            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
            text.clear();
            reject(webSocket, 1003, "Binary messages are not supported.");
            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onPing(WebSocket webSocket, ByteBuffer message) {
            // The JDK sends the corresponding pong automatically.
            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onPong(WebSocket webSocket, ByteBuffer message) {
            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            text.clear();
            stop(false);
            // Completing this callback lets the JDK send the reciprocal close frame.
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            text.clear();
            stop(true);
        }

        private void dispatch(WebSocket webSocket, String message) {
            if (closed.get()) {
                return;
            }
            try {
                String response = WebSocketProtocol.respond(dispatcher, message);
                if (!closed.get() && !Thread.currentThread().isInterrupted()) {
                    // A single worker waits for each send, so no two sendText calls overlap.
                    webSocket.sendText(response, true).get();
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException | RuntimeException exception) {
                stop(true);
            }
        }

        private void reject(WebSocket webSocket, int status, String reason) {
            if (!closed.compareAndSet(false, true)) {
                return;
            }
            worker.shutdownNow();
            try {
                webSocket
                        .sendClose(status, reason)
                        .orTimeout(5, TimeUnit.SECONDS)
                        .whenComplete((ignored, failure) -> webSocket.abort());
            } catch (RuntimeException exception) {
                webSocket.abort();
            }
        }

        private void stop(boolean abort) {
            closed.set(true);
            worker.shutdownNow();
            CompletableFuture<WebSocket> pending = connecting;
            if (pending != null && !pending.isDone()) {
                pending.cancel(true);
            }
            WebSocket current = socket;
            if (abort && current != null) {
                current.abort();
            }
        }
    }
}
