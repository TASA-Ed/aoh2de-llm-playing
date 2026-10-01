package top.tasaed.aoh2de.llm.playing.modes;

import com.alibaba.fastjson2.JSON;
import io.javalin.Javalin;
import top.tasaed.aoh2de.llm.playing.core.LPConfig;

public final class HttpServerTransport implements Transport {
    private final LPConfig config;
    private final Javalin server;
    private volatile boolean running;

    public HttpServerTransport(LPConfig config, ApiDispatcher dispatcher) {
        this.config = config;
        server = Javalin.create(javalin -> {
            javalin.startup.showJavalinBanner = false;
            javalin.routes.before(ctx -> {
                byte[] body = ctx.req().getInputStream().readNBytes(ApiDispatcher.MAX_REQUEST_BODY_SIZE + 1);
                ApiResponse response = dispatcher.dispatchJson(ctx.method().name(), ctx.path(), body);
                ctx.status(response.status());
                if (response.status() == 405) {
                    ctx.header("Allow", "/v1/health".equals(ctx.path()) ? "GET" : "POST");
                }
                if (response.body() instanceof String text) {
                    ctx.contentType("text/plain; charset=utf-8").result(text);
                } else {
                    ctx.contentType("application/json; charset=utf-8").result(JSON.toJSONBytes(response.body()));
                }
                ctx.skipRemainingHandlers();
            });
        });
    }

    @Override
    public void start() {
        try {
            server.start(config.getHost(), config.getPort());
            running = true;
        } catch (RuntimeException exception) {
            server.stop();
            throw exception;
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public void close() {
        running = false;
        server.stop();
    }
}
