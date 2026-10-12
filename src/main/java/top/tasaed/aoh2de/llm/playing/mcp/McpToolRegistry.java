package top.tasaed.aoh2de.llm.playing.mcp;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;
import top.tasaed.aoh2de.llm.playing.modes.ApiDispatcher;
import top.tasaed.aoh2de.llm.playing.modes.ApiResponse;

public final class McpToolRegistry {
    private static final JsonMapper MAPPER = new JsonMapper();
    private final ApiDispatcher dispatcher;
    private final Map<String, Binding> tools;
    private final List<McpServerFeatures.SyncToolSpecification> specifications;

    private record Binding(String path, McpToolDefinition definition) {}

    public McpToolRegistry(ApiDispatcher dispatcher, Set<ToolKind> allowedKinds) {
        this.dispatcher = Objects.requireNonNull(dispatcher, "dispatcher");
        Set<ToolKind> kinds = Set.copyOf(allowedKinds);
        if (kinds.isEmpty()) {
            throw new IllegalArgumentException("At least one MCP tool kind must be enabled");
        }
        Map<String, Binding> selected = new HashMap<>();
        Set<String> names = new HashSet<>();
        dispatcher.handlers().forEach((path, handler) -> {
            if (handler instanceof McpToolProvider provider) {
                McpToolDefinition definition = Objects.requireNonNull(provider.mcpToolDefinition(), "tool definition");
                if (!names.add(definition.name())) {
                    throw new IllegalArgumentException("Duplicate MCP tool name: " + definition.name());
                }
                if (kinds.contains(definition.toolKind())) {
                    selected.put(definition.name(), new Binding(path, definition));
                }
            }
        });
        tools = Map.copyOf(selected);
        List<McpServerFeatures.SyncToolSpecification> registered = new ArrayList<>(tools.size());
        tools.values().stream()
                .sorted(Comparator.comparing(binding -> binding.definition().name()))
                .forEach(binding -> {
                    McpToolDefinition definition = binding.definition();
                    boolean readOnly = definition.toolKind() == ToolKind.READ_ONLY;
                    McpSchema.Tool tool = McpSchema.Tool.builder(definition.name(), definition.inputSchema())
                            .description(definition.description())
                            .annotations(McpSchema.ToolAnnotations.builder()
                                    .readOnlyHint(readOnly)
                                    .destructiveHint(!readOnly)
                                    .idempotentHint(readOnly)
                                    .openWorldHint(false)
                                    .build())
                            .meta(Map.of("toolKind", definition.toolKind().value()))
                            .build();
                    registered.add(McpServerFeatures.SyncToolSpecification.builder()
                            .tool(tool)
                            .callHandler((exchange, request) -> call(definition.name(), request.arguments()))
                            .build());
                });
        specifications = List.copyOf(registered);
    }

    public List<McpServerFeatures.SyncToolSpecification> specifications() {
        return specifications;
    }

    public McpSchema.CallToolResult call(String name, Map<String, Object> arguments) {
        Binding binding = name == null ? null : tools.get(name);
        if (binding == null) {
            return adapt(ApiResponse.error(404, "TOOL_NOT_FOUND", "No enabled MCP tool matches this name."));
        }
        ObjectNode request = arguments == null ? MAPPER.createObjectNode() : MAPPER.valueToTree(arguments);
        // The dispatcher retains lifecycle checks and the handler retains all game-state validation
        // and main-thread scheduling. Schemas describe inputs; they do not replace business checks.
        return adapt(dispatcher.dispatch("POST", binding.path(), request));
    }

    private static McpSchema.CallToolResult adapt(ApiResponse response) {
        return McpSchema.CallToolResult.builder()
                .isError(response.status() >= 400)
                .structuredContent(response.body())
                .addTextContent(MAPPER.writeValueAsString(response.body()))
                .meta(Map.of("httpStatus", response.status()))
                .build();
    }
}
