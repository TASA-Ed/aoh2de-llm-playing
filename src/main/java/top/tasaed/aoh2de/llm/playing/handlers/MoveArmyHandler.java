package top.tasaed.aoh2de.llm.playing.handlers;

import age.of.civilizations2.jakowski.lukasz.CFG;
import age.of.civilizations2.jakowski.lukasz.Civilization;
import age.of.civilizations2.jakowski.lukasz.GameAction;
import age.of.civilizations2.jakowski.lukasz.MoveUnitsB.MoveUnits;
import age.of.civilizations2.jakowski.lukasz.Province;
import age.of.civilizations2.jakowski.lukasz.RegroupArmy.RegroupArmy;
import com.alibaba.fastjson2.JSONObject;
import java.util.ArrayList;
import java.util.List;
import top.tasaed.aoh2de.llm.playing.HttpResponses;

public final class MoveArmyHandler extends GameRequestHandler {
    public MoveArmyHandler() {
        super("MOVE_ARMY_FAILED", "Failed to move the army.");
    }

    @Override
    protected JSONObject handleOnGameThread(JSONObject request) {
        Integer fromProvinceId;
        Integer toProvinceId;
        Integer units;
        try {
            fromProvinceId = request.getInteger("fromProvinceId");
            toProvinceId = request.getInteger("toProvinceId");
            units = request.getInteger("units");
        } catch (RuntimeException exception) {
            return HttpResponses.error("INVALID_PARAMETER", "Province IDs and units must be integers.");
        }
        boolean moveTo = request.getBooleanValue("moveTo");

        if (fromProvinceId == null || toProvinceId == null || units == null) {
            return HttpResponses.error("MISSING_PARAMETER", "fromProvinceId, toProvinceId and units are required.");
        }
        if (CFG.gameAction.getActiveTurnStateID() != GameAction.TurnStates.INPUT_ORDERS) {
            return HttpResponses.error("NOT_ACCEPTING_ORDERS", "The game is not accepting orders now.");
        }
        if (!isValidProvinceId(fromProvinceId) || !isValidProvinceId(toProvinceId)) {
            return HttpResponses.error("INVALID_PROVINCE_ID", "A province ID is out of range.");
        }
        if (fromProvinceId.intValue() == toProvinceId.intValue()) {
            return HttpResponses.error("SAME_PROVINCE", "The source and target provinces must differ.");
        }
        if (units <= 0) {
            return HttpResponses.error("INVALID_UNIT_COUNT", "units must be greater than zero.");
        }

        int civilizationId = CFG.core.getPlayer(CFG.PLAYER_TURN_ID).getCivId();
        Civilization civilization = CFG.core.getCiv(civilizationId);
        Province source = CFG.core.getProv(fromProvinceId);
        int availableUnits = source.getArmyCivID1(civilizationId);
        if (availableUnits <= 0) {
            return HttpResponses.error("NO_CONTROLLED_ARMY", "The player has no army in the source province.");
        }
        RegroupArmy route = new RegroupArmy(civilizationId, fromProvinceId, toProvinceId);
        if (route.getRouteSize() <= 0) {
            return HttpResponses.error("NO_VALID_ROUTE", "No valid route to the target province was found.");
        }
        if (!moveTo && route.getRouteSize() != 1) {
            return HttpResponses.error(
                    "TARGET_NOT_ADJACENT", "The target is not adjacent. Set moveTo to true to use a multi-turn route.");
        }

        int firstHopProvinceId = route.getRoute(0);
        MoveUnits existingOrder = findOrder(civilization, fromProvinceId, firstHopProvinceId);
        int maximumUnits = availableUnits;
        if (!moveTo && existingOrder != null) {
            maximumUnits += existingOrder.getNumberOfUnits();
        }
        if (units > maximumUnits) {
            return HttpResponses.error("NOT_ENOUGH_UNITS", "The source province does not have enough units.");
        }

        int movementCost = CFG.gameAction.costOfMoveArmy(fromProvinceId, firstHopProvinceId, civilizationId);
        boolean adjustsExistingOrder = CFG.gameAction.getIsFreeMove(civilizationId, fromProvinceId, firstHopProvinceId);
        if (!adjustsExistingOrder && civilization.getMovemPoints() < movementCost) {
            return HttpResponses.error(
                    "NOT_ENOUGH_MOVEMENT_POINTS", "The civilization does not have enough movement points.");
        }

        boolean accepted =
                CFG.gameAction.moveArmyAction(fromProvinceId, firstHopProvinceId, units, civilizationId, moveTo, true);
        if (!accepted) {
            return HttpResponses.error("MOVE_REJECTED", "The game rejected the army movement order.");
        }

        List<Integer> remainingRoute = new ArrayList<>();
        if (moveTo && route.getRouteSize() > 1) {
            route.setFromProvinceID(firstHopProvinceId);
            route.removeRoute(0);
            route.setNumOfUnits(units);
            civilization.addRegroupArmy(route);
            for (int i = 0; i < route.getRouteSize(); i++) {
                remainingRoute.add(route.getRoute(i));
            }
        }

        CFG.core.getPlayer(CFG.PLAYER_TURN_ID).setNoOrders(false);
        CFG.menus.updateInGameTopAll(civilizationId);

        JSONObject result = new JSONObject();
        result.put("civilizationId", civilizationId);
        result.put("fromProvinceId", fromProvinceId);
        result.put("toProvinceId", toProvinceId);
        result.put("firstHopProvinceId", firstHopProvinceId);
        result.put("units", units);
        result.put("moveTo", moveTo);
        result.put("movementCost", adjustsExistingOrder ? 0 : movementCost);
        result.put("remainingMovementPoints", civilization.getMovemPoints());
        result.put("remainingRoute", remainingRoute);
        return HttpResponses.success(result);
    }

    private static boolean isValidProvinceId(int provinceId) {
        return provinceId >= 0 && provinceId < CFG.core.getProvinSize();
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
}
