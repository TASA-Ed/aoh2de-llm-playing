package top.tasaed.aoh2de.llm.playing.core;

import java.io.IOException;
import top.tasaed.aoh2de.llm.playing.modes.*;

public final class LP {
    public static final String VERSION = "0.2.0";
    private Transport transport;
    private ApiDispatcher dispatcher;

    private LP() {}

    private static class Holder {
        private static final LP INSTANCE = new LP();
    }

    public static LP getInstance() {
        return Holder.INSTANCE;
    }

    public synchronized void start(LPConfig config) throws IOException {
        if (isRunning()) {
            return;
        }
        config.validate();
        stop();
        ApiDispatcher newDispatcher = new ApiDispatcher();
        Transport newTransport = null;
        try {
            newTransport = switch (config.getMode()) {
                case "http-server" -> new HttpServerTransport(config, newDispatcher);
                case "ws-server" -> new WebSocketServerTransport(config, newDispatcher);
                case "ws-client" -> new WebSocketClientTransport(config, newDispatcher);
                default -> throw new IllegalArgumentException("Unsupported mode: " + config.getMode());
            };
            newTransport.start();
            dispatcher = newDispatcher;
            transport = newTransport;
        } catch (IOException | RuntimeException exception) {
            newDispatcher.close();
            if (newTransport != null) {
                newTransport.close();
            }
            throw exception;
        }
    }

    public synchronized boolean isRunning() {
        return transport != null && transport.isRunning();
    }

    public synchronized void stop() {
        // Release workers waiting on the game thread before stopping a network server.
        if (dispatcher != null) {
            dispatcher.close();
            dispatcher = null;
        }
        if (transport != null) {
            try {
                transport.close();
            } finally {
                transport = null;
            }
        }
    }
}
