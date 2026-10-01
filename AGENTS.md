# LLM Playing 项目指南

本项目为 Finality Framework 插件，为《历史时代 2：DE》（Age of History II: Definitive Edition）游戏提供了 LLM Playing HTTP API。

可参考 `ref/` 下的源代码，`ref/aoh2de/` 为游戏代码，`ref/loader/` 为 Finality Framework Loader 代码。（部分环境下可能没有此文件夹）

## 项目概览

- 游戏启动后按配置启动一种传输模式，供 LLM Agent、脚本或其他工具调用；HTTP / WS 服务端使用 Javalin，WS 客户端使用 `java.net.http.WebSocket`。
- 提供国家、地区、军队、外交、建筑和回合等事件查询与操作。
- 三种模式复用相同的路由与业务处理；所有游戏读写都切换到游戏主线程执行，避免直接从网络线程访问游戏状态。

## 项目结构

- `src/main/`
  - `resources/plugin.json` Finality Framework 插件元数据。
  - `java/top/tasaed/aoh2de/llm/playing/`
    - `core/` 核心逻辑。
      - `LP.java` 启动服务，按配置启动一种传输模式。
      - `LPConfig.java` 配置类。
    - `mixins/` Mixin 游戏逻辑。
      - `MixinAoCGame.java` Inject 游戏启动类，用于调用 `LP.java`。
    - `modes` 传输模式逻辑。
      - `ApiDispatcher.java` API 调度集合
    - `handlers/` 路由/方法处理。
    - `FileUtil.java` 文件工具。
- `lib/` Finality Framework Loader（`loader.jar`），游戏（`game.jar`） 依赖。
- `openapi.yaml` 路由的 OpenAPI 文档。

## 规范

- 添加或编辑路由后，务必同时编辑 OpenAPI 文档，使用 `./gradlew.bat lintOpenApi` 检测格式。 
- 编辑代码后运行 `./gradlew.bat spotlessApply` 和 `./gradlew.bat build` 保证代码无误。
- 路由一般以 `/v1` 开头，如：`/v1/message/action_message`。

## 与游戏联调

因为游戏运行需要 Gdx.files 和对应资源才可运行，所以与游戏联合调试需要在 Steam 游戏目录下进行，不要引用 `libs/` 下的 jar 进行。

因为需要打包依赖，所以首先运行 `./gradlew.bat clean release`，生成的 all jar 包需要放入 `[游戏目录]/mods/[模组名称]/plugins/` 中。

然后运行游戏目录下的 Finality Loader（命名一般类似 `loader-1.x.x.jar`），然后等待游戏加载，可能需要较长时间。

如果用户没有提供 Steam 游戏目录和模组名称，则默认无需调试。
