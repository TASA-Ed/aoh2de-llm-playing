package top.tasaed.aoh2de.llm.playing.mcp;

import java.util.Map;
import java.util.Objects;

public record McpToolDefinition(String name, String description, Map<String, Object> inputSchema, ToolKind toolKind) {
    public McpToolDefinition {
        if (name == null || !name.matches("[A-Za-z0-9_.-]{1,128}")) {
            throw new IllegalArgumentException("Invalid MCP tool name");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("MCP tool description must not be blank");
        }
        Objects.requireNonNull(inputSchema, "inputSchema");
        if (!"object".equals(inputSchema.get("type"))) {
            throw new IllegalArgumentException("MCP tool inputSchema must describe an object");
        }
        inputSchema = Map.copyOf(inputSchema);
        Objects.requireNonNull(toolKind, "toolKind");
    }
}
