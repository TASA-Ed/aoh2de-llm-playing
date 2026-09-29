package top.tasaed.aoh2de.llm.playing;

import java.net.URI;
import java.net.URISyntaxException;

public class LPConfig {
    private String mode = "http-server";
    private String host = "127.0.0.1";
    private int port = 8080;
    private String wsPath = "/ws";
    private String wsUrl = "ws://127.0.0.1:8080/ws";

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

    public void validate() {
        if (!"http-server".equals(mode) && !"ws-server".equals(mode) && !"ws-client".equals(mode)) {
            throw new IllegalArgumentException("mode must be http-server, ws-server, or ws-client");
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
            validateWsPath();
        }
    }

    private void validateWsPath() {
        if (wsPath == null || wsPath.isBlank()) {
            throw new IllegalArgumentException("wsPath must be an absolute nonwildcard endpoint path");
        }
        try {
            URI path = new URI(wsPath);
            String decoded = path.getPath();
            if (!wsPath.startsWith("/")
                    || wsPath.startsWith("//")
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
                throw new IllegalArgumentException("wsPath must be an absolute nonwildcard endpoint path");
            }
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("wsPath must be an absolute nonwildcard endpoint path", e);
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
}
