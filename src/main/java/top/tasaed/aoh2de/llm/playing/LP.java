package top.tasaed.aoh2de.llm.playing;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import top.tasaed.aoh2de.llm.playing.handlers.*;

public final class LP {
    private static final byte[] HEALTH_RESPONSE = "OK".getBytes(StandardCharsets.UTF_8);

    private HttpServer server;
    private ExecutorService executor;

    private LP() {}

    private static class Holder {
        private static final LP INSTANCE = new LP();
    }

    public static LP getInstance() {
        return Holder.INSTANCE;
    }

    public synchronized void start(String host, int port) throws IOException {
        if (server != null) {
            return;
        }

        InetSocketAddress address = new InetSocketAddress(host, port);
        ExecutorService newExecutor = Executors.newCachedThreadPool(new DaemonThreadFactory());
        HttpServer newServer = null;
        boolean started = false;
        try {
            newServer = HttpServer.create();
            newServer.setExecutor(newExecutor);
            registerRoutes(newServer);
            newServer.bind(address, 0);
            newServer.start();
            server = newServer;
            executor = newExecutor;
            started = true;
        } finally {
            if (!started) {
                try {
                    if (newServer != null) {
                        newServer.stop(0);
                    }
                } finally {
                    newExecutor.shutdownNow();
                }
            }
        }
    }

    private void registerRoutes(HttpServer newServer) {
        newServer.createContext("/v1/health", this::handleHealth);
        newServer.createContext("/v1/army/move", new MoveArmyHandler());
        newServer.createContext("/v1/army/cancel_move", new CancelArmyMoveHandler());
        newServer.createContext("/v1/army/get_army_list", new ArmyListHandler());
        newServer.createContext("/v1/building/construct", new ConstructBuildingHandler());
        newServer.createContext("/v1/self/set_budget_spending", new BudgetSpendingHandler());
        newServer.createContext("/v1/self/get_budget_spending_info", new BudgetSpendingInfoHandler());
        newServer.createContext("/v1/self/get_civilization_view", new CivilizationViewHandler());
        newServer.createContext("/v1/diplomacy/get_stats", new DiplomacyStatsHandler());
        newServer.createContext("/v1/diplomacy/declare_war", new DeclareWarHandler());
        newServer.createContext("/v1/diplomacy/change_relation", new ChangeRelationHandler());
        newServer.createContext("/v1/event/get_current_event", new CurrentEventHandler());
        newServer.createContext("/v1/message/get_message_list", new MessageListHandler());
        newServer.createContext("/v1/message/action_message", new MessageActionHandler());
        newServer.createContext("/v1/nation/get_nation_information", new NationInformationHandler());
        newServer.createContext("/v1/nation/get_province_list", new ProvinceListHandler());
        newServer.createContext("/v1/nation/get_neighbor_civs", new NeighborCivsHandler());
        newServer.createContext("/v1/province/get_information", new ProvinceInformationHandler());
        newServer.createContext("/v1/self/get_summary", new SelfSummaryHandler());
        newServer.createContext("/v1/turn/click_end_turn", new EndTurnHandler());
        newServer.createContext("/v1/turn/get_stats", new TurnStatsHandler());
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

        try {
            server.stop(0);
        } finally {
            try {
                executor.shutdownNow();
            } finally {
                server = null;
                executor = null;
            }
        }
    }

    private void handleHealth(HttpExchange exchange) throws IOException {
        HttpResponses.send(exchange, 200, "text/plain; charset=utf-8", HEALTH_RESPONSE);
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
