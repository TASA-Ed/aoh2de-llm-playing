package top.tasaed.aoh2de.llm.playing.core;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import top.tasaed.aoh2de.llm.playing.mcp.ToolKind;

public class LPConfig {
    private String mode = "http-server";
    private String host = "127.0.0.1";
    private int port = 8080;
    private String wsPath = "/ws";
    private String wsUrl = "ws://127.0.0.1:8080/ws";
    private String mcpPath = "/mcp";
    private List<String> mcpToolKinds = List.of("read-only", "write");

    public LPConfig() {}

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getWsPath() {
        return wsPath;
    }

    public void setWsPath(String wsPath) {
        this.wsPath = wsPath;
    }

    public String getWsUrl() {
        return wsUrl;
    }

    public void setWsUrl(String wsUrl) {
        this.wsUrl = wsUrl;
    }

    public String getMcpPath() {
        return mcpPath;
    }

    public void setMcpPath(String mcpPath) {
        this.mcpPath = mcpPath;
    }

    @JsonProperty("mcpToolKinds")
    public void setMcpToolKinds(List<String> mcpToolKinds) {
        this.mcpToolKinds = mcpToolKinds;
    }

    @JsonIgnore
    public Set<ToolKind> getMcpToolKinds() {
        if (mcpToolKinds == null || mcpToolKinds.isEmpty()) {
            throw new IllegalArgumentException("mcpToolKinds must be a nonempty array of read-only or write");
        }
        Set<ToolKind> kinds = EnumSet.noneOf(ToolKind.class);
        for (String kind : mcpToolKinds) {
            if ("read-only".equals(kind)) {
                kinds.add(ToolKind.READ_ONLY);
            } else if ("write".equals(kind)) {
                kinds.add(ToolKind.WRITE);
            } else {
                throw new IllegalArgumentException("mcpToolKinds must contain only read-only or write");
            }
        }
        return Set.copyOf(kinds);
    }

    public void validate() {
        if (!"http-server".equals(mode)
                && !"ws-server".equals(mode)
                && !"ws-client".equals(mode)
                && !"mcp-server".equals(mode)) {
            throw new IllegalArgumentException("mode must be http-server, ws-server, ws-client, or mcp-server");
        }
        if ("ws-client".equals(mode)) {
            validateWsUrl();
            return;
        }
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("host must not be blank");
        }
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("port must be between 1 and 65535");
        }
        if ("ws-server".equals(mode)) {
            validateEndpointPath("wsPath", wsPath);
        }
        if ("mcp-server".equals(mode)) {
            validateEndpointPath("mcpPath", mcpPath);
            getMcpToolKinds();
        }
    }

    private static void validateEndpointPath(String field, String endpoint) {
        String message = field + " must be an absolute nonwildcard endpoint path";
        if (endpoint == null || endpoint.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        try {
            URI path = new URI(endpoint);
            String decoded = path.getPath();
            if (!endpoint.startsWith("/")
                    || endpoint.startsWith("//")
                    || path.isAbsolute()
                    || path.getRawAuthority() != null
                    || path.getRawQuery() != null
                    || path.getRawFragment() != null
                    || decoded == null
                    || decoded.indexOf('*') >= 0
                    || decoded.indexOf('{') >= 0
                    || decoded.indexOf('}') >= 0
                    || decoded.indexOf('<') >= 0
                    || decoded.indexOf('>') >= 0
                    || decoded.indexOf('\\') >= 0
                    || decoded.chars().anyMatch(Character::isISOControl)) {
                throw new IllegalArgumentException(message);
            }
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(message, e);
        }
    }

    private void validateWsUrl() {
        if (wsUrl == null || wsUrl.isBlank()) {
            throw new IllegalArgumentException("wsUrl must be an absolute ws or wss URL");
        }
        try {
            URI url = new URI(wsUrl);
            if ((!"ws".equalsIgnoreCase(url.getScheme()) && !"wss".equalsIgnoreCase(url.getScheme()))
                    || !url.isAbsolute()
                    || url.getHost() == null
                    || url.getHost().isBlank()
                    || url.getRawFragment() != null
                    || url.getRawUserInfo() != null
                    || url.getPort() < -1
                    || url.getPort() == 0
                    || url.getPort() > 65535) {
                throw new IllegalArgumentException(
                        "wsUrl must be an absolute ws or wss URL with a host and no userinfo or fragment");
            }
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("wsUrl must be an absolute ws or wss URL", e);
        }
    }

    public String getAddress() {
        if ("ws-client".equals(this.mode)) {
            return this.wsUrl;
        }
        String host = this.host;
        if (host != null && host.indexOf(':') >= 0 && !host.startsWith("[")) {
            host = "[" + host + "]";
        }
        boolean isWsServer = "ws-server".equals(this.mode);
        String scheme = isWsServer ? "ws://" : "http://";
        String path = isWsServer && this.wsPath != null ? this.wsPath : "";
        if ("mcp-server".equals(this.mode)) {
            path = this.mcpPath;
        }
        return scheme + host + ":" + this.port + path;
    }
}
