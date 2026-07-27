package top.tasaed.aoh2de.llm.playing.handlers;

import top.tasaed.aoh2de.llm.playing.HttpResponses;

import com.alibaba.fastjson2.JSONObject;
import com.badlogic.gdx.Gdx;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

abstract class GameRequestHandler implements HttpHandler {
    private final String failureCode;
    private final String failureMessage;

    public GameRequestHandler(String failureCode, String failureMessage) {
        this.failureCode = failureCode;
        this.failureMessage = failureMessage;
    }

    @Override
    public final void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.getResponseHeaders().set("Allow", "POST");
            HttpResponses.sendJson(exchange, 405,
                    HttpResponses.error("METHOD_NOT_ALLOWED", "Use POST for this endpoint."));
            return;
        }

        if (Gdx.app == null) {
            HttpResponses.sendJson(exchange, 503,
                    HttpResponses.error("GAME_NOT_READY", "The game application is not ready."));
            return;
        }

        CompletableFuture<JSONObject> result = new CompletableFuture<>();
        Gdx.app.postRunnable(() -> {
            try {
                result.complete(handleOnGameThread());
            } catch (Throwable throwable) {
                result.completeExceptionally(throwable);
            }
        });

        try {
            JSONObject response = result.get();
            HttpResponses.sendJson(exchange, response.getBooleanValue("success") ? 200 : 409, response);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            HttpResponses.sendJson(exchange, 500,
                    HttpResponses.error("REQUEST_INTERRUPTED", "The request was interrupted."));
        } catch (ExecutionException exception) {
            exception.getCause().printStackTrace();
            HttpResponses.sendJson(exchange, 500, HttpResponses.error(failureCode, failureMessage));
        }
    }

    protected abstract JSONObject handleOnGameThread();
}
