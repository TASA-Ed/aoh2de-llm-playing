package top.tasaed.aoh2de.llm.playing;

public record ApiResponse(int status, Object body) {
    public static ApiResponse error(int status, String code, String message) {
        return new ApiResponse(status, HttpResponses.error(code, message));
    }
}
