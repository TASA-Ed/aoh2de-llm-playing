package top.tasaed.aoh2de.llm.playing.modes;

import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

public final class HttpResponses {
    private HttpResponses() {}

    /** 操作成功，无信息 */
    public static ObjectNode success() {
        ObjectNode response = JsonNodeFactory.instance.objectNode();
        response.put("success", true);
        return response;
    }

    /**
     * 操作成功，附带信息
     * @param result 返回 JSON 响应
     */
    public static ObjectNode success(ObjectNode result) {
        ObjectNode response = success();
        response.set("result", result);
        return response;
    }

    /**
     * 操作失败
     * @param code 状态码
     * @param message 状态消息
     */
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
