package top.tasaed.aoh2de.llm.playing.modes;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONObject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class WebSocketProtocol {
    private WebSocketProtocol() {}

    static String respond(ApiDispatcher dispatcher, String message) {
        Object parsed;
        try {
            parsed = JSON.parse(message);
        } catch (JSONException exception) {
            return error(null, "INVALID_JSON", "The WebSocket message must be valid JSON.");
        }
        if (!(parsed instanceof JSONObject request)) {
            return error(null, "INVALID_REQUEST", "The WebSocket message must be a JSON object.");
        }
        Object rawId = request.get("id");
        if (!(rawId instanceof String id) || id.isEmpty()) {
            return error(null, "INVALID_REQUEST", "id must be a nonempty string.");
        }
        if (!(request.get("method") instanceof String method)) {
            return error(id, "INVALID_REQUEST", "method must be a string.");
        }
        if (!(request.get("path") instanceof String path)) {
            return error(id, "INVALID_REQUEST", "path must be a string.");
        }
        JSONObject body;
        if (!request.containsKey("body")) {
            body = new JSONObject();
        } else if (request.get("body") instanceof JSONObject object) {
            body = object;
        } else {
            return error(id, "INVALID_REQUEST", "body must be a JSON object when supplied.");
        }
        return envelope(id, dispatcher.dispatch(method, path, body));
    }

    private static String error(String id, String code, String message) {
        return envelope(id, new ApiResponse(400, HttpResponses.error(code, message)));
    }

    private static String envelope(String id, ApiResponse response) {
        // Serialize the fields explicitly so an unavailable id is present as JSON null.
        return "{\"id\":" + JSON.toJSONString(id) + ",\"status\":" + response.status() + ",\"body\":"
                + JSON.toJSONString(response.body()) + "}";
    }

    static ExecutorService newWorker(String name) {
        return Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, name);
            thread.setDaemon(true);
            return thread;
        });
    }

    static final class TextAccumulator {
        private final StringBuilder text = new StringBuilder();
        private int bytes;
        private boolean highSurrogate;

        boolean append(CharSequence fragment) {
            for (int index = 0; index < fragment.length(); index++) {
                char character = fragment.charAt(index);
                if (highSurrogate && Character.isLowSurrogate(character)) {
                    // A pair takes four UTF-8 bytes; its high surrogate already counted three.
                    bytes++;
                } else {
                    bytes += character < 0x80 ? 1 : character < 0x800 ? 2 : 3;
                }
                highSurrogate = Character.isHighSurrogate(character);
                if (bytes > ApiDispatcher.MAX_REQUEST_BODY_SIZE) {
                    clear();
                    return false;
                }
            }
            text.append(fragment);
            return true;
        }

        String take() {
            String message = text.toString();
            clear();
            return message;
        }

        void clear() {
            text.setLength(0);
            bytes = 0;
            highSurrogate = false;
        }
    }
}
