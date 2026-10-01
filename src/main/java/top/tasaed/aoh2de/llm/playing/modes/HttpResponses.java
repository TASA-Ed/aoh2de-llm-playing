package top.tasaed.aoh2de.llm.playing.modes;

import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

public final class HttpResponses {
    private HttpResponses() {}

    public static ObjectNode success() {
        ObjectNode response = JsonNodeFactory.instance.objectNode();
        response.put("success", true);
        return response;
    }

    public static ObjectNode success(ObjectNode result) {
        ObjectNode response = success();
        response.set("result", result);
        return response;
    }

    public static ObjectNode error(String code, String message) {
        ObjectNode response = JsonNodeFactory.instance.objectNode();
        response.put("success", false);

        ObjectNode error = JsonNodeFactory.instance.objectNode();
        error.put("code", code);
        error.put("message", message);
        response.set("error", error);
        return response;
    }
}
