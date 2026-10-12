# LLM Playing

让 LLM 游玩《历史时代 2：DE》（Age of History II: Definitive Edition）。支持 HTTP 服务端、WebSocket 服务端、WebSocket 客户端和 MCP Streamable HTTP 服务端四种运行模式。

QQ 群：[597524393](https://qm.qq.com/q/nC2N5Y1UX0)。

本项目为 Finality Framework 插件，依赖于 [Finality Loader](https://github.com/Finality-Framework/loader) 启动。

如果你需要 Agent，可以使用 [LLM Playing Agent](https://github.com/TASA-Ed/aoh2de-llm-playing-agent)。

```mermaid
flowchart TB

    subgraph A["LLM Playing Agent"]
        A1["Node.js"]
        A4["velin-react<br/>加载 TSX 提示词"]
        A5["注册 LLM Tools"]
        A6["Agent Loop"]
        A7["Vercel AI SDK 请求模型"]
        A8["LLM"]
        A9["LLM 用工具"]
        A10["LLM 结束回合"]
        A11["LLM 返回回合总结到 CLI"]

        A1 --> A4 --> A5 --> A6
        A6 --> A7 --> A8
        A8 --> A9 --> A6
        A8 --> A10 --> A11 --> A6
    end

    subgraph B["aoh2de-llm-playing"]
        B1["finality-framework 框架加载"]
        B3["MixinAoCGame.java<br/>注入 create 方法"]
        B4["按配置启动 HTTP / MCP 服务端<br/>或 WebSocket 服务端 / 客户端"]
        B5["ApiDispatcher.java<br/>统一路由与主线程调度"]

        B6["/v1/turn/click_end_turn<br/>EndTurnHandler.java"]
        B7["/v1/nation/get_nation_information<br/>NationInformationHandler.java"]
        B8["/v1/self/get_summary<br/>SelfSummaryHandler.java"]
        B9["..."]

        B1 --> B3 --> B4 --> B5
        B5 --> B6
        B5 --> B7
        B5 --> B8
        B5 --> B9
    end

    A9 -->|"LLM Tool 调用"| B4
    B4 -->|"HTTP / WebSocket / MCP"| A9
```

## 功能

- 游戏启动后按配置启动一种传输模式，供 LLM Agent、脚本或其他工具调用；HTTP / WS / MCP 服务端使用 Javalin，WS 客户端使用 `java.net.http.WebSocket`。
- 提供国家、地区、军队、外交、建筑和回合等事件查询与操作。
- 四种模式复用相同的业务处理；HTTP / WS 使用路由，MCP 使用 Handler 提供的独立工具元数据。所有游戏读写都切换到游戏主线程执行，避免直接从网络线程访问游戏状态。
- 配置读取及 JSON 编解码使用 Jackson 3；业务处理器使用原生 `ObjectNode` / `ArrayNode`。MCP 使用[官方 Java SDK](https://github.com/modelcontextprotocol/java-sdk/tree/v2.0.1) `io.modelcontextprotocol.sdk:mcp:2.0.1`（Jackson 3），通过 Javalin 7.2.3 / Jetty Jakarta Servlet 挂载官方 Streamable HTTP transport，不自定义 JSON-RPC 协议。

## 需求

- [《Age of History II: Definitive Edition》](https://store.steampowered.com/app/3381680/Age_of_History_2_Definitive_Edition/)
- [Finality Loader](https://github.com/Finality-Framework/loader) 1.6.0+
- Java 17 或更高版本

## 使用

在创意工坊中订阅 [LLM Playing](https://steamcommunity.com/sharedfiles/filedetails/?id=3785324635)，随后使用 [Finality Loader](https://github.com/Finality-Framework/loader) 启动游戏。

在游戏根目录创建 `LP_Config.json`（注意文件名大小写），通过 `mode` 选择运行模式。配置在游戏启动时读取，修改后需重启游戏；一次只运行一种模式。

### HTTP 服务端（默认）

```json
{
  "mode": "http-server",
  "host": "127.0.0.1",
  "port": 8080
}
```

使用 Javalin 监听 `http://127.0.0.1:8080`，保留原有 `/v1` HTTP API。没有配置文件，或旧配置只有 `host` / `port` 时，默认使用此模式。

```sh
curl http://127.0.0.1:8080/v1/health
curl -X POST http://127.0.0.1:8080/v1/self/get_summary -H "Content-Type: application/json" -d '{}'
```

### WebSocket 服务端

```json
{
  "mode": "ws-server",
  "host": "127.0.0.1",
  "port": 8080,
  "wsPath": "/ws"
}
```

使用 Javalin 监听 `ws://127.0.0.1:8080/ws`。Agent 主动连接插件，通过下文的 JSON 消息协议调用接口；此模式不提供 HTTP 游戏接口，包括 HTTP 健康检查。

### WebSocket 客户端

```json
{
  "mode": "ws-client",
  "wsUrl": "ws://127.0.0.1:9000/ws"
}
```

插件使用 `java.net.http.WebSocket` 主动连接 `wsUrl` 指定的远端服务（支持 `ws://` 和 `wss://`），不监听本地端口。远端服务发送操作请求，插件执行后沿同一连接返回结果；不是将本地 HTTP 请求转发到远端。

远端服务应先于游戏启动。连接超时为 10 秒；连接失败或断开后不会自动重连，也不会重试游戏操作。恢复远端服务后需重启游戏建立连接。

### MCP Streamable HTTP 服务端

```json
{
  "mode": "mcp-server",
  "host": "127.0.0.1",
  "port": 8080,
  "mcpPath": "/mcp",
  "mcpToolKinds": ["read-only", "write"]
}
```

使用支持[标准 MCP Streamable HTTP](https://modelcontextprotocol.io/specification/2025-06-18/basic/transports#streamable-http) 的客户端，将传输类型设为 Streamable HTTP / HTTP，端点 URL 设为 `http://127.0.0.1:8080/mcp`。本模式仅提供 MCP，不提供 `/v1` 路由或 HTTP 健康检查。

只读访问可设 `"mcpToolKinds": ["read-only"]`；仅启用操作可设 `["write"]`。默认两类都启用。筛选在启动时完成，被排除的工具既不出现在 `tools/list` 中，也不能通过 `tools/call` 调用；工具的 `_meta.toolKind` 与 `annotations.readOnlyHint` 可供客户端识别类型。只读筛选不是身份认证。

SDK 负责初始化、会话、协议版本协商、GET / POST / DELETE 和流式响应。整个 MCP HTTP 请求体（含 JSON-RPC 信封）上限为 64 KiB。Origin / Host 由 SDK 官方安全校验器检查，仅接受配置端点的主机和端口，以及同源 Origin（非浏览器客户端通常不发送 Origin）；拒绝任意外部 Origin，不应通过关闭检查来解决连接错误。修改 host 时，应使用客户端实际连接的可信主机名或 IP，而非任意通配绑定地址。

### 配置字段与启动失败

| 字段 | 默认值 | 生效模式 |
| --- | --- | --- |
| `mode` | `http-server` | 全部；仅接受 `http-server`、`ws-server`、`ws-client`、`mcp-server` |
| `host` | `127.0.0.1` | HTTP / WS / MCP 三种服务端模式 |
| `port` | `8080` | 三种服务端模式；范围 1–65535 |
| `wsPath` | `/ws` | WS 服务端；绝对路径，不支持通配符、查询参数或片段 |
| `wsUrl` | `ws://127.0.0.1:8080/ws` | WS 客户端；包含主机的完整 URL，不支持用户信息或片段 |
| `mcpPath` | `/mcp` | MCP 服务端；与 wsPath 相同的绝对非通配路径校验，不支持查询参数或片段 |
| `mcpToolKinds` | `["read-only", "write"]` | MCP 服务端；非空 JSON 字符串数组，仅接受精确值 `read-only` / `write` |

启动模式及地址会输出到 Finality Loader 日志（MCP 模式输出完整端点 URL）。配置文件存在但为空、格式错误或当前模式字段无效时，不启动任何传输，也不会回退到 HTTP。MCP 空工具类别数组、null 或未知类别会拒绝启动；端口占用或 WS 连接失败同样会记录启动错误。

服务没有内置身份认证。服务端默认只监听本机；如需修改监听地址，请限制为可信网络。WS 客户端只应连接可信服务，因为远端可以控制游戏。

游戏退出时会关闭服务或连接，并取消尚未执行的游戏请求；已经开始执行的操作不会被中断或回滚。

可选用 [LLM Playing Agent](https://github.com/TASA-Ed/aoh2de-llm-playing-agent)；使用 WS 模式时，调用方需支持本文的消息协议；MCP 模式可使用任意支持 Streamable HTTP 的标准 MCP 客户端。

## 构建

1. 将游戏 JAR 放到 `libs/game.jar`。
2. 将 Finality Loader JAR 放到 `libs/`。本仓库已经提供，你也可以自行选择替换最新版本（如有）。
3. 在项目根目录执行：

```powershell
.\gradlew release
```

## 使用 API

HTTP 接口、路径和业务参数参考 [openapi.yaml](./openapi.yaml)。两种 WS 模式复用同一套路径、方法、参数和业务响应。MCP 通过 `tools/list` 返回的工具 schema 发现能力，而不是将 `/v1` 路径用作 JSON-RPC 方法。

### HTTP 通用约定

- 健康检查 `GET /v1/health` 返回纯文本 `OK`，即使游戏尚未可操作也可用于确认服务已启动。
- 除健康检查外，所有接口均为 `POST`，使用 UTF-8 JSON 请求和响应。
- 空请求体等同于 `{}`，需要参数的接口仍会验证必填字段。
- 请求体最大为 64 KiB。
- 成功响应为 `{"success":true}` 或 `{"success":true,"result":{...}}`，失败响应为 `{"success":false,"error":{"code":"...","message":"..."}}`。
- HTTP `409` 表示当前游戏状态不允许该操作，例如不在可下达命令的回合阶段，`503` 表示游戏尚未准备完成。
- 建议不要并发提交多个操作。

### MCP 工具发现与调用

1. 客户端连接 MCP URL，发送 `initialize` 并完成 `notifications/initialized`，保存 SDK 返回的会话标识；标准客户端会自动处理会话头、协议版本头和 SSE 流。
2. 调用 `tools/list`，获得允许使用的工具 `name`、`description`、`inputSchema`、类型元数据及 annotations；按返回的 schema 组织参数，不从 HTTP 路径猜工具名。工具发现和初始化不依赖游戏是否已进入可操作状态。
3. 调用 `tools/call`，参数为 `{"name": 工具列表中的名称, "arguments": 业务参数对象}`；不传 `method` / `path` / `body` 的 WS 信封。无参数工具使用 `{}`。业务参数校验仍由原 Handler 执行。
4. 读取结果的 `structuredContent`：保留原业务响应 `{"success":true,"result":...}` 或 `{"success":false,"error":{"code":"...","message":"..."}}`。`content` 同时包含此对象的 JSON 文本，供不支持结构化结果的客户端使用。业务失败会设置 `isError: true`，并在 `_meta.httpStatus` 中保留原业务状态码；不要把它误当作 MCP HTTP 连接失败。

例如，游戏尚未启动到可操作状态时，游戏工具调用会返回：

```json
{
  "isError": true,
  "structuredContent": {
    "success": false,
    "error": {
      "code": "GAME_NOT_READY",
      "message": "The game application is not ready."
    }
  },
  "content": [{"type": "text", "text": "{\"success\":false,\"error\":{\"code\":\"GAME_NOT_READY\",\"message\":\"The game application is not ready.\"}}"}],
  "_meta": {"httpStatus": 503}
}
```

这是 `tools/call` 的结果内容，不是完整 JSON-RPC 信封。协议格式错误、未注册的工具名等协议错误由 SDK 处理，不保证带业务 `structuredContent`。建议按顺序调用游戏工具；关闭插件时先取消尚未执行的游戏工作，再关闭 MCP 会话和网络服务。已经执行的操作不会回滚，未收到结果时不要盲目重试写操作。

### WebSocket 消息协议

WS 服务端与客户端使用相同协议；插件始终是请求的执行方，区别仅在于谁发起连接。每条请求是一个 UTF-8 JSON 文本消息：

```json
{
  "id": "request-1",
  "method": "POST",
  "path": "/v1/self/get_summary",
  "body": {}
}
```

- `id`：必填非空字符串；调用方应保证同一连接内未完成请求的 ID 唯一，用于匹配响应，不用于去重。
- `method`：必填字符串；健康检查为 `GET`，其他接口为 `POST`。
- `path`：必填字符串，精确匹配 OpenAPI 中的路径，不附带查询参数。
- `body`：业务参数对象；省略等同于 `{}`，不能为数组、字符串或 `null`。
- 整条请求消息（包括信封）最大 64 KiB，按 UTF-8 字节计算；支持分片文本消息，不支持二进制消息。

响应信封固定包含 `id`、`status` 和 `body`。例如，游戏尚未准备完成：

```json
{
  "id": "request-1",
  "status": 503,
  "body": {
    "success": false,
    "error": {
      "code": "GAME_NOT_READY",
      "message": "The game application is not ready."
    }
  }
}
```

`status` 对应 HTTP 状态码，`body` 保留原有接口响应对象。成功的游戏操作为 `200`，业务拒绝为 `409`；未知路径为 `404`，方法错误为 `405`，消息 JSON 或信封格式错误为 `400`。无法提取有效 ID 时返回 `"id": null`。

健康检查不依赖游戏状态：

```json
{"id":"health-1","method":"GET","path":"/v1/health"}
```

```json
{"id":"health-1","status":200,"body":"OK"}
```

例如，在支持原生 WebSocket 的 JavaScript 环境中连接 WS 服务端：

```javascript
const socket = new WebSocket('ws://127.0.0.1:8080/ws');
socket.addEventListener('open', () => {
  socket.send(JSON.stringify({ id: 'health-1', method: 'GET', path: '/v1/health' }));
});
socket.addEventListener('message', event => console.log(JSON.parse(event.data)));
```

单个连接按接收顺序处理请求。多个连接仍共享游戏主线程，建议收到响应后再发送下一条游戏操作。连接断开会取消该连接尚未执行的请求，但不能回滚已经执行的操作；未收到响应不代表操作一定未执行，不要盲目重试。

超大消息关闭连接，关闭码为 `1009`；二进制消息关闭码为 `1003`（WS 服务端对超大的二进制消息优先使用 `1009`）。

## 开发

```powershell
# 编译与打包
.\gradlew.bat build

# 生成用于发布的 shadow JAR 与源码 JAR
.\gradlew.bat release

# 格式化 Java 源码
.\gradlew.bat spotlessApply

# 校验 OpenAPI 文档（需要可用的 pnpm / node 环境）
.\gradlew.bat lintOpenApi
```

提交接口变更时应同时更新 `openapi.yaml` 和 `mcp/HandlerMcpTools.java` 中对应的 `inputSchema`，确保定义与 Handler 参数一致。

新增 MCP 工具时：

- Handler 选择性实现 `McpToolProvider`，通过 `mcpToolDefinition()` 返回独立的 `McpToolDefinition(name, description, inputSchema, toolKind)`；不把执行逻辑写进元数据接口。
- 在 `ApiDispatcher` 注册一次 Handler 实例；`McpToolRegistry` 从现有路由绑定建立自己的工具索引，不创建第二套 Handler。未实现接口的 Handler 不会暴露为 MCP 工具。
- `ToolKind.READ_ONLY` / `WRITE` 决定启动时筛选及 MCP 注解；筛选同时限制发现和执行。JSON Schema 描述参数结构，主线程调度、参数兼容性和动态业务校验仍保留在 Handler 中。

## 许可证

本项目采用 [AGPL 3.0](./LICENSE) 许可证。
