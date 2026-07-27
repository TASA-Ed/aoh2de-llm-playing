package top.tasaed.aoh2de.llm.playing;

import top.tasaed.aoh2de.llm.playing.handlers.EndTurnHandler;
import top.tasaed.aoh2de.llm.playing.handlers.NationInformationHandler;
import top.tasaed.aoh2de.llm.playing.handlers.NationSummaryHandler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public final class LP {
    public static final int PORT = 8088;

    private HttpServer server;
    private ExecutorService executor;

    private LP() {
    }

    private static class Holder {
        private static final LP INSTANCE = new LP();
    }

    public static LP getInstance() {
        return Holder.INSTANCE;
    }

    public synchronized void start() throws IOException {
        if (server != null) {
            return;
        }

        HttpServer newServer = HttpServer.create(new InetSocketAddress("127.0.0.1", PORT), 0);
        ExecutorService newExecutor = Executors.newCachedThreadPool(new DaemonThreadFactory());
        newServer.setExecutor(newExecutor);
        newServer.createContext("/health", this::handleHealth);
        newServer.createContext("/end_turn", new EndTurnHandler());
        newServer.createContext("/get_nation_summary", new NationSummaryHandler());
        newServer.createContext("/get_nation_information", new NationInformationHandler());

        try {
            newServer.start();
            server = newServer;
            executor = newExecutor;
        } catch (RuntimeException exception) {
            newExecutor.shutdownNow();
            throw exception;
        }
    }

    public synchronized HttpServer getServer() {
        if (server == null) {
            throw new IllegalStateException("HTTP server has not been started");
        }
        return server;
    }

    public synchronized boolean isRunning() {
        return server != null;
    }

    public synchronized void stop() {
        if (server == null) {
            return;
        }

        server.stop(0);
        executor.shutdownNow();
        server = null;
        executor = null;
    }

    private void handleHealth(HttpExchange exchange) throws IOException {
        byte[] response = "OK".getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(200, response.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(response);
        }
    }

    private static final class DaemonThreadFactory implements ThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "lp-http-" + threadNumber.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    }
}
