package top.tasaed.aoh2de.llm.playing.handlers;

import age.of.civilizations2.jakowski.lukasz.CFG;
import age.of.civilizations2.jakowski.lukasz.Civilization;
import age.of.civilizations2.jakowski.lukasz.GameAction;
import age.of.civilizations2.jakowski.lukasz.MoveUnitsB.MoveUnits;
import age.of.civilizations2.jakowski.lukasz.RegroupArmy.RegroupArmy;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;
import top.tasaed.aoh2de.llm.playing.modes.HttpResponses;

public final class CancelArmyMoveHandler extends GameRequestHandler {
    public CancelArmyMoveHandler() {
        super("CANCEL_ARMY_MOVE_FAILED", "Failed to cancel the army movement order.");
    }

    @Override
    protected ObjectNode handleOnGameThread(ObjectNode request) {
        Integer fromProvinceId;
        Integer toProvinceId;
        try {
            fromProvinceId = request.hasNonNull("fromProvinceId")
                    ? request.get("fromProvinceId").asInt()
                    : null;
            toProvinceId = request.hasNonNull("toProvinceId")
                    ? request.get("toProvinceId").asInt()
                    : null;
        } catch (RuntimeException exception) {
            return HttpResponses.error("INVALID_PARAMETER", "Province IDs must be integers.");
        }
        if (fromProvinceId == null || toProvinceId == null) {
            return HttpResponses.error("MISSING_PARAMETER", "fromProvinceId and toProvinceId are required.");
        }
        if (CFG.gameAction.getActiveTurnStateID() != GameAction.TurnStates.INPUT_ORDERS) {
            return HttpResponses.error("NOT_ACCEPTING_ORDERS", "The game is not accepting orders now.");
        }
        if (!isValidProvinceId(fromProvinceId) || !isValidProvinceId(toProvinceId)) {
            return HttpResponses.error("INVALID_PROVINCE_ID", "A province ID is out of range.");
        }

        int civilizationId = CFG.core.getPlayer(CFG.PLAYER_TURN_ID).getCivId();
        Civilization civilization = CFG.core.getCiv(civilizationId);
        MoveUnits order = findOrder(civilization, fromProvinceId, toProvinceId);
        if (order == null) {
            order = findRoutedOrder(civilization, fromProvinceId, toProvinceId);
        }
        if (order == null) {
            return HttpResponses.error("MOVE_NOT_FOUND", "No matching army movement order was found.");
        }

        int queuedToProvinceId = order.getToProvID();
        int canceledUnits = order.getNumberOfUnits();
        CFG.gameAction.moveArmyAction(fromProvinceId, queuedToProvinceId, 0, civilizationId, true, true);
        if (findOrder(civilization, fromProvinceId, queuedToProvinceId) != null) {
            return HttpResponses.error("CANCEL_REJECTED", "The game did not cancel the movement order.");
        }

        CFG.menus.updateInGameTopAll(civilizationId);

        ObjectNode result = JsonNodeFactory.instance.objectNode();
        result.put("civilizationId", civilizationId);
        result.put("fromProvinceId", fromProvinceId);
        result.put("toProvinceId", toProvinceId);
        result.put("queuedToProvinceId", queuedToProvinceId);
        result.put("canceledUnits", canceledUnits);
        result.put("remainingMovementPoints", civilization.getMovemPoints());
        return HttpResponses.success(result);
    }

    private static MoveUnits findOrder(Civilization civilization, int fromProvinceId, int toProvinceId) {
        for (int i = 0; i < civilization.moveUnitsSize(); i++) {
            MoveUnits order = civilization.getMoveUnits(i);
            if (order.getFromProviID() == fromProvinceId && order.getToProvID() == toProvinceId) {
                return order;
            }
        }
        return null;
    }

    private static MoveUnits findRoutedOrder(Civilization civilization, int fromProvinceId, int finalProvinceId) {
        for (int i = 0; i < civilization.moveUnitsSize(); i++) {
            MoveUnits order = civilization.getMoveUnits(i);
            if (order.getFromProviID() != fromProvinceId) {
                continue;
            }
            for (int j = 0; j < civilization.getRegroupArmySize(); j++) {
                RegroupArmy route = civilization.getRegroupArmy(j);
                if (route.getFromProvinceID() == order.getToProvID()
                        && route.getRouteSize() > 0
                        && route.getToProvinceID() == finalProvinceId) {
                    return order;
                }
            }
        }
        return null;
    }

    private static boolean isValidProvinceId(int provinceId) {
        return provinceId >= 0 && provinceId < CFG.core.getProvinSize();
    }
}
