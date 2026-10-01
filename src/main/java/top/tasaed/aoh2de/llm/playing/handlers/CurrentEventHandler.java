package top.tasaed.aoh2de.llm.playing.handlers;

import age.of.civilizations2.jakowski.lukasz.CFG;
import age.of.civilizations2.jakowski.lukasz.MoveUnitsB.MoveUnits;
import age.of.civilizations2.jakowski.lukasz.MoveUnitsB.MoveUnits_TurnData;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;
import top.tasaed.aoh2de.llm.playing.modes.HttpResponses;

public final class CurrentEventHandler extends GameRequestHandler {
    public CurrentEventHandler() {
        super("GET_CURRENT_EVENT_FAILED", "Failed to get current event.");
    }

    @Override
    protected ObjectNode handleOnGameThread(ObjectNode request) {
        MoveUnits_TurnData currentMove = CFG.gameAction.getCurrentMoveunits();

        if (currentMove == null) {
            return HttpResponses.error("NO_EVENT", "No current event");
        }

        ArrayNode armies = JsonNodeFactory.instance.arrayNode();
        int totalTroops = 0;
        int playerId = CFG.core.getPlayer(CFG.PLAYER_TURN_ID).getCivId();

        for (int i = 0; i < currentMove.getMoveUnitsSize(); i++) {
            MoveUnits army = currentMove.getMoveUnits(i);
            ObjectNode armyInfo = JsonNodeFactory.instance.objectNode();

            int attackerCivId = currentMove.getCivID(i);

            armyInfo.put("attackerCivId", attackerCivId);
            armyInfo.put("fromProvinceId", army.getFromProviID());
            armyInfo.put("toProvinceId", army.getToProvID());
            armyInfo.put("troops", army.getNumberOfUnits());
            armyInfo.put("isPlayer", attackerCivId == playerId);

            totalTroops += army.getNumberOfUnits();
            armies.add(armyInfo);
        }

        ObjectNode result = JsonNodeFactory.instance.objectNode();
        result.set("armies", armies);
        result.put("totalAttackingTroops", totalTroops);
        result.put(
                "defenderCivID",
                CFG.core.getProv(currentMove.getMoveUnits(0).getToProvID()).getCivId());

        return HttpResponses.success(result);
    }
}
