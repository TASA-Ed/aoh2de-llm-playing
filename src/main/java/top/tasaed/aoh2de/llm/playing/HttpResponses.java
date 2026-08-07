package top.tasaed.aoh2de.llm.playing;

import com.alibaba.fastjson2.JSONObject;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public final class HttpResponses {
    private HttpResponses() {}

    public static JSONObject success() {
        JSONObject response = new JSONObject();
        response.put("success", true);
        return response;
    }

    public static JSONObject success(JSONObject result) {
        JSONObject response = success();
        response.put("result", result);
        return response;
    }

    public static JSONObject error(String code, String message) {
        JSONObject response = new JSONObject();
        response.put("success", false);

        JSONObject error = new JSONObject();
        error.put("code", code);
        error.put("message", message);
        response.put("error", error);
        return response;
    }

    public static void sendJson(HttpExchange exchange, int statusCode, JSONObject response) throws IOException {
        byte[] body = response.toJSONString().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, body.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(body);
        }
    }
}
