package top.tasaed.aoh2de.llm.playing.modes;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.cfg.JsonNodeFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

final class WebSocketProtocol {
    private static final JsonMapper MAPPER =
            JsonMapper.builder().disable(JsonNodeFeature.WRITE_NULL_PROPERTIES).build();

    private WebSocketProtocol() {}

    static String respond(ApiDispatcher dispatcher, String message) {
        JsonNode parsed;
        try {
            parsed = MAPPER.readTree(message);
        } catch (JacksonException exception) {
            return error(null, "INVALID_JSON", "The WebSocket message must be valid JSON.");
        }
        if (!(parsed instanceof ObjectNode request)) {
            return error(null, "INVALID_REQUEST", "The WebSocket message must be a JSON object.");
        }
        JsonNode rawId = request.path("id");
        if (!rawId.isString() || rawId.stringValue().isEmpty()) {
            return error(null, "INVALID_REQUEST", "id must be a nonempty string.");
        }
        String id = rawId.stringValue();
        JsonNode method = request.path("method");
        if (!method.isString()) {
            return error(id, "INVALID_REQUEST", "method must be a string.");
        }
        JsonNode path = request.path("path");
        if (!path.isString()) {
            return error(id, "INVALID_REQUEST", "path must be a string.");
        }
        ObjectNode body;
        if (!request.has("body")) {
            body = MAPPER.createObjectNode();
        } else if (request.get("body") instanceof ObjectNode object) {
            body = object;
        } else {
            return error(id, "INVALID_REQUEST", "body must be a JSON object when supplied.");
        }
        return envelope(id, dispatcher.dispatch(method.stringValue(), path.stringValue(), body));
    }

    private static String error(String id, String code, String message) {
        return envelope(id, new ApiResponse(400, HttpResponses.error(code, message)));
    }

    private static String envelope(String id, ApiResponse response) {
        // Serialize the fields explicitly so an unavailable id is present as JSON null.
        return "{\"id\":" + MAPPER.writeValueAsString(id) + ",\"status\":" + response.status() + ",\"body\":"
                + MAPPER.writeValueAsString(response.body()) + "}";
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
