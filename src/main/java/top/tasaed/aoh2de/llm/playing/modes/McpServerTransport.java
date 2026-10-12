package top.tasaed.aoh2de.llm.playing.modes;

import io.javalin.Javalin;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.json.schema.jackson3.DefaultJsonSchemaValidator;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.DefaultServerTransportSecurityValidator;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import java.net.URI;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import tools.jackson.databind.json.JsonMapper;
import top.tasaed.aoh2de.llm.playing.core.LP;
import top.tasaed.aoh2de.llm.playing.core.LPConfig;
import top.tasaed.aoh2de.llm.playing.mcp.McpToolRegistry;

public final class McpServerTransport implements Transport {
    private final LPConfig config;
    private final McpToolRegistry registry;
    private Javalin server;
    private McpSyncServer mcpServer;
    private HttpServletStreamableServerTransportProvider provider;
    private volatile boolean running;

    public McpServerTransport(LPConfig config, ApiDispatcher dispatcher) {
        this.config = config;
        this.registry = new McpToolRegistry(dispatcher, config.getMcpToolKinds());
    }

    @Override
    public void start() {
        JsonMapper mapper = JsonMapper.builder().build();
        JacksonMcpJsonMapper mcpMapper = new JacksonMcpJsonMapper(mapper);
        URI endpoint = URI.create(config.getAddress());
        // The SDK servlet's default validator is NOOP; use its official Host/Origin validator.
        var securityValidator = DefaultServerTransportSecurityValidator.builder()
                .allowedHost(endpoint.getRawAuthority())
                .allowedOrigin(endpoint.getScheme() + "://" + endpoint.getRawAuthority())
                .build();
        try {
            provider = HttpServletStreamableServerTransportProvider.builder()
                    .jsonMapper(mcpMapper)
                    .mcpEndpoint(config.getMcpPath())
                    .maxRequestSize(ApiDispatcher.MAX_REQUEST_BODY_SIZE)
                    .securityValidator(securityValidator)
                    .build();
            mcpServer = McpServer.sync(provider)
                    .jsonMapper(mcpMapper)
                    .jsonSchemaValidator(new DefaultJsonSchemaValidator(mapper))
                    // Keep business parameter validation and its structured errors in the handlers.
                    .validateToolInputs(false)
                    .serverInfo("LLM Playing", LP.VERSION)
                    .capabilities(
                            McpSchema.ServerCapabilities.builder().tools(false).build())
                    .tools(registry.specifications())
                    .build();
            server = Javalin.create(javalin -> {
                javalin.startup.showJavalinBanner = false;
                javalin.jetty.modifyServletContextHandler(context -> {
                    ServletHolder servlet = new ServletHolder(provider);
                    servlet.setAsyncSupported(true);
                    // Servlet "/" is a default mapping; "" is the exact context-root mapping.
                    context.addServlet(servlet, "/".equals(config.getMcpPath()) ? "" : config.getMcpPath());
                });
            });
            server.start(config.getHost(), config.getPort());
            running = true;
        } catch (RuntimeException exception) {
            try {
                close();
            } catch (RuntimeException closeException) {
                exception.addSuppressed(closeException);
            }
            throw exception;
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public void close() {
        running = false;
        try {
            if (mcpServer != null) {
                McpSyncServer closing = mcpServer;
                mcpServer = null;
                provider = null;
                closing.closeGracefully();
            } else if (provider != null) {
                HttpServletStreamableServerTransportProvider closing = provider;
                provider = null;
                closing.closeGracefully().block();
            }
        } finally {
            if (server != null) {
                Javalin closing = server;
                server = null;
                closing.stop();
            }
        }
    }
}
