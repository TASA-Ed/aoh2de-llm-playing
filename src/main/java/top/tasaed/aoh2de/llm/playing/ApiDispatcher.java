package top.tasaed.aoh2de.llm.playing;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import java.util.Map;
import top.tasaed.aoh2de.llm.playing.handlers.*;

public final class ApiDispatcher implements AutoCloseable {
    public static final int MAX_REQUEST_BODY_SIZE = 64 * 1024;
    private static final String HEALTH_PATH = "/v1/health";
    private final Map<String, GameRequestHandler> routes = Map.ofEntries(
            Map.entry("/v1/army/move", new MoveArmyHandler()),
            Map.entry("/v1/army/cancel_move", new CancelArmyMoveHandler()),
            Map.entry("/v1/army/get_army_list", new ArmyListHandler()),
            Map.entry("/v1/building/construct", new ConstructBuildingHandler()),
            Map.entry("/v1/self/set_budget_spending", new BudgetSpendingHandler()),
            Map.entry("/v1/self/get_budget_spending_info", new BudgetSpendingInfoHandler()),
            Map.entry("/v1/self/get_civilization_view", new CivilizationViewHandler()),
            Map.entry("/v1/diplomacy/get_stats", new DiplomacyStatsHandler()),
            Map.entry("/v1/diplomacy/declare_war", new DeclareWarHandler()),
            Map.entry("/v1/diplomacy/change_relation", new ChangeRelationHandler()),
            Map.entry("/v1/event/get_current_event", new CurrentEventHandler()),
            Map.entry("/v1/message/get_message_list", new MessageListHandler()),
            Map.entry("/v1/message/action_message", new MessageActionHandler()),
            Map.entry("/v1/nation/get_nation_information", new NationInformationHandler()),
            Map.entry("/v1/nation/get_province_list", new ProvinceListHandler()),
            Map.entry("/v1/nation/get_neighbor_civs", new NeighborCivsHandler()),
            Map.entry("/v1/province/get_information", new ProvinceInformationHandler()),
            Map.entry("/v1/self/get_summary", new SelfSummaryHandler()),
            Map.entry("/v1/turn/click_end_turn", new EndTurnHandler()),
            Map.entry("/v1/turn/get_stats", new TurnStatsHandler()));
    private volatile boolean closed;

    public ApiResponse dispatchJson(String method, String path, byte[] body) {
        ApiResponse early = checkRoute(method, path);
        if (early != null) {
            return early;
        }
        if (body.length > MAX_REQUEST_BODY_SIZE) {
            return ApiResponse.error(413, "REQUEST_BODY_TOO_LARGE", "The request body must not exceed 64 KiB.");
        }
        final JSONObject request;
        try {
            request = body.length == 0 ? new JSONObject() : JSON.parseObject(body);
            if (request == null) {
                throw new IllegalArgumentException("Expected a JSON object");
            }
        } catch (RuntimeException exception) {
            return ApiResponse.error(400, "INVALID_JSON", "The request body must be a valid JSON object.");
        }
        return routes.get(path).handle(request);
    }

    public ApiResponse dispatch(String method, String path, JSONObject body) {
        ApiResponse early = checkRoute(method, path);
        return early != null ? early : routes.get(path).handle(body == null ? new JSONObject() : body);
    }

    private ApiResponse checkRoute(String method, String path) {
        if (closed) {
            return ApiResponse.error(503, "SERVICE_STOPPED", "The service has stopped.");
        }
        boolean health = HEALTH_PATH.equals(path);
        if (!health && (path == null || !routes.containsKey(path))) {
            return ApiResponse.error(404, "NOT_FOUND", "No endpoint matches this path.");
        }
        String allowed = health ? "GET" : "POST";
        if (!allowed.equals(method)) {
            return ApiResponse.error(405, "METHOD_NOT_ALLOWED", "Use " + allowed + " for this endpoint.");
        }
        return health ? new ApiResponse(200, "OK") : null;
    }

    @Override
    public void close() {
        closed = true;
        routes.values().forEach(GameRequestHandler::close);
    }
}
