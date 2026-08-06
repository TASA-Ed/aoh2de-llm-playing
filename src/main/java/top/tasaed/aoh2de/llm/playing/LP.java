package top.tasaed.aoh2de.llm.playing;

import top.tasaed.aoh2de.llm.playing.handlers.CancelArmyMoveHandler;
import top.tasaed.aoh2de.llm.playing.handlers.EndTurnHandler;
import top.tasaed.aoh2de.llm.playing.handlers.MoveArmyHandler;
import top.tasaed.aoh2de.llm.playing.handlers.NationInformationHandler;
import top.tasaed.aoh2de.llm.playing.handlers.NationSummaryHandler;
import top.tasaed.aoh2de.llm.playing.handlers.ProvinceListHandler;
import top.tasaed.aoh2de.llm.playing.handlers.ArmyListHandler;
import top.tasaed.aoh2de.llm.playing.handlers.ConstructBuildingHandler;

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

    public synchronized void start(int port) throws IOException {
        if (server != null) {
            return;
        }

        HttpServer newServer = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        ExecutorService newExecutor = Executors.newCachedThreadPool(new DaemonThreadFactory());
        newServer.setExecutor(newExecutor);
        newServer.createContext("/v1/health", this::handleHealth);
        newServer.createContext("/v1/turn/end_turn", new EndTurnHandler());
        newServer.createContext("/v1/army/move", new MoveArmyHandler());
        newServer.createContext("/v1/army/cancel_move", new CancelArmyMoveHandler());
        newServer.createContext("/v1/army/get_army_list", new ArmyListHandler());
        newServer.createContext("/v1/building/construct", new ConstructBuildingHandler());
        newServer.createContext("/v1/nation/get_nation_summary", new NationSummaryHandler());
        newServer.createContext("/v1/nation/get_nation_information", new NationInformationHandler());
        newServer.createContext("/v1/nation/get_province_list", new ProvinceListHandler());

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
