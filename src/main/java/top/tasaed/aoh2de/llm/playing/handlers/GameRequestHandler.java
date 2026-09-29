package top.tasaed.aoh2de.llm.playing.handlers;

import com.alibaba.fastjson2.JSONObject;
import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import top.tasaed.aoh2de.llm.playing.ApiResponse;

public abstract class GameRequestHandler {
    private final String failureCode;
    private final String failureMessage;
    private final Set<FutureTask<JSONObject>> pending = ConcurrentHashMap.newKeySet();
    private volatile boolean closed;

    public GameRequestHandler(String failureCode, String failureMessage) {
        this.failureCode = failureCode;
        this.failureMessage = failureMessage;
    }

    public final ApiResponse handle(JSONObject request) {
        Application application = Gdx.app;
        if (application == null) {
            return ApiResponse.error(503, "GAME_NOT_READY", "The game application is not ready.");
        }

        FutureTask<JSONObject> result = new FutureTask<>(() -> handleOnGameThread(request));
        pending.add(result);
        try {
            if (closed) {
                result.cancel(false);
            } else {
                application.postRunnable(result);
            }
            JSONObject response = result.get();
            return new ApiResponse(response.getBooleanValue("success") ? 200 : 409, response);
        } catch (InterruptedException exception) {
            // Cancel queued work, never interrupt or roll back an operation already executing.
            result.cancel(false);
            Thread.currentThread().interrupt();
            return ApiResponse.error(500, "REQUEST_INTERRUPTED", "The request was interrupted.");
        } catch (CancellationException exception) {
            return ApiResponse.error(503, "SERVICE_STOPPED", "The service has stopped.");
        } catch (ExecutionException exception) {
            exception.getCause().printStackTrace();
            return ApiResponse.error(500, failureCode, failureMessage);
        } catch (RuntimeException exception) {
            result.cancel(false);
            exception.printStackTrace();
            return ApiResponse.error(500, failureCode, failureMessage);
        } finally {
            pending.remove(result);
        }
    }

    public final void close() {
        closed = true;
        pending.forEach(result -> result.cancel(false));
    }

    protected abstract JSONObject handleOnGameThread(JSONObject request);
}
