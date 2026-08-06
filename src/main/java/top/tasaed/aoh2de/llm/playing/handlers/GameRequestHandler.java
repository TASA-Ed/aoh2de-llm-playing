package top.tasaed.aoh2de.llm.playing.handlers;

import top.tasaed.aoh2de.llm.playing.HttpResponses;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.badlogic.gdx.Gdx;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

abstract class GameRequestHandler implements HttpHandler {
    private static final int MAX_REQUEST_BODY_SIZE = 64 * 1024;

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

        final JSONObject request;
        try {
            request = readRequest(exchange);
        } catch (RequestBodyTooLargeException exception) {
            HttpResponses.sendJson(exchange, 413,
                    HttpResponses.error("REQUEST_BODY_TOO_LARGE", "The request body must not exceed 64 KiB."));
            return;
        } catch (RuntimeException exception) {
            HttpResponses.sendJson(exchange, 400,
                    HttpResponses.error("INVALID_JSON", "The request body must be a valid JSON object."));
            return;
        }

        CompletableFuture<JSONObject> result = new CompletableFuture<>();
        Gdx.app.postRunnable(() -> {
            try {
                result.complete(handleOnGameThread(request));
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

    protected abstract JSONObject handleOnGameThread(JSONObject request);

    private static JSONObject readRequest(HttpExchange exchange) throws IOException {
        try (InputStream input = exchange.getRequestBody();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int total = 0;
            int read;
            while ((read = input.read(buffer)) != -1) {
                total += read;
                if (total > MAX_REQUEST_BODY_SIZE) {
                    throw new RequestBodyTooLargeException();
                }
                output.write(buffer, 0, read);
            }
            if (total == 0) {
                return new JSONObject();
            }
            JSONObject request = JSON.parseObject(output.toByteArray());
            if (request == null) {
                throw new IllegalArgumentException("The request body is not a JSON object");
            }
            return request;
        }
    }

    private static final class RequestBodyTooLargeException extends RuntimeException {
    }
}
