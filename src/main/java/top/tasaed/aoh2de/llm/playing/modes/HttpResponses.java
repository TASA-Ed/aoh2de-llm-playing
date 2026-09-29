package top.tasaed.aoh2de.llm.playing.modes;

import com.alibaba.fastjson2.JSONObject;

public final class HttpResponses {
    private HttpResponses() {

    }

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
}
