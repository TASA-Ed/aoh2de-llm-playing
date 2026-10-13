package top.tasaed.aoh2de.llm.playing.handlers;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import team.rainfall.finality.FinalityLogger;
import tools.jackson.databind.node.ObjectNode;
import top.tasaed.aoh2de.llm.playing.modes.ApiResponse;

/** 执行和游戏线程调度逻辑 */
public abstract class GameRequestHandler {
    private final String failureCode;
    private final String failureMessage;
    private final Set<FutureTask<ObjectNode>> pending = ConcurrentHashMap.newKeySet();
    private volatile boolean closed;

    /**
     * 默认失败文本
     * @param failureCode 失败状态码
     * @param failureMessage 失败消息
     */
    public GameRequestHandler(String failureCode, String failureMessage) {
        this.failureCode = failureCode;
        this.failureMessage = failureMessage;
    }

    public final ApiResponse handle(ObjectNode request) {
        Application application = Gdx.app;
        if (application == null) {
            return ApiResponse.error(503, "GAME_NOT_READY", "The game application is not ready.");
        }

        FutureTask<ObjectNode> result = new FutureTask<>(() -> handleOnGameThread(request));
        pending.add(result);
        try {
            if (closed) {
                result.cancel(false);
            } else {
                application.postRunnable(result);
            }
            ObjectNode response = result.get();
            return new ApiResponse(response.path("success").asBoolean() ? 200 : 409, response);
        } catch (InterruptedException exception) {
            // Cancel queued work, never interrupt or roll back an operation already executing.
            result.cancel(false);
            Thread.currentThread().interrupt();
            FinalityLogger.error("[LP] The request was interrupted:", exception);
            return ApiResponse.error(500, "REQUEST_INTERRUPTED", "The request was interrupted.");
        } catch (CancellationException exception) {
            FinalityLogger.error("[LP] The service has stopped:", exception);
            return ApiResponse.error(503, "SERVICE_STOPPED", "The service has stopped.");
        } catch (ExecutionException exception) {
            FinalityLogger.error("[LP] Execution Exception:", exception);
            return ApiResponse.error(500, failureCode, failureMessage);
        } catch (RuntimeException exception) {
            result.cancel(false);
            FinalityLogger.error("[LP] Runtime Exception:", exception);
            return ApiResponse.error(500, failureCode, failureMessage);
        } finally {
            pending.remove(result);
        }
    }

    public final void close() {
        closed = true;
        pending.forEach(result -> result.cancel(false));
    }

    /**
     * 执行逻辑
     * @param request JSON 请求体
     * @return 返回 JSON 响应体
     */
    protected abstract ObjectNode handleOnGameThread(ObjectNode request);
}
