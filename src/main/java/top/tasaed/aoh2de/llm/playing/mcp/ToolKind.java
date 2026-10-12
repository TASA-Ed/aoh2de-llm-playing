package top.tasaed.aoh2de.llm.playing.mcp;

public enum ToolKind {
    READ_ONLY("read-only"),
    WRITE("write");

    private final String value;

    ToolKind(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
