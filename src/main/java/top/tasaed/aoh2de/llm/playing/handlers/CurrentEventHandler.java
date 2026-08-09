package top.tasaed.aoh2de.llm.playing.handlers;

import age.of.civilizations2.jakowski.lukasz.CFG;
import age.of.civilizations2.jakowski.lukasz.MoveUnitsB.MoveUnits;
import age.of.civilizations2.jakowski.lukasz.MoveUnitsB.MoveUnits_TurnData;
import com.alibaba.fastjson2.JSONObject;
import java.util.ArrayList;
import java.util.List;
import top.tasaed.aoh2de.llm.playing.HttpResponses;

public final class CurrentEventHandler extends GameRequestHandler {
    public CurrentEventHandler() {
        super("GET_CURRENT_EVENT_FAILED", "Failed to get current event.");
    }

    @Override
    protected JSONObject handleOnGameThread(JSONObject request) {
        MoveUnits_TurnData currentMove = CFG.gameAction.getCurrentMoveunits();

        if (currentMove == null) {
            return HttpResponses.error("NO_EVENT", "No current event");
        }

        List<JSONObject> armies = new ArrayList<>();
        int totalTroops = 0;
        int playerId = CFG.core.getPlayer(CFG.PLAYER_TURN_ID).getCivId();

        for (int i = 0; i < currentMove.getMoveUnitsSize(); i++) {
            MoveUnits army = currentMove.getMoveUnits(i);
            JSONObject armyInfo = new JSONObject();

            int attackerCivId = currentMove.getCivID(i);

            armyInfo.put("attackerCivId", attackerCivId);
            armyInfo.put("fromProvinceId", army.getFromProviID());
            armyInfo.put("toProvinceId", army.getToProvID());
            armyInfo.put("troops", army.getNumberOfUnits());
            armyInfo.put("isPlayer", attackerCivId == playerId);

            totalTroops += army.getNumberOfUnits();
            armies.add(armyInfo);
        }

        JSONObject result = new JSONObject();
        result.put("armies", armies);
        result.put("totalAttackingTroops", totalTroops);
        result.put(
                "defenderCivID",
                CFG.core.getProv(currentMove.getMoveUnits(0).getToProvID()).getCivId());

        return HttpResponses.success(result);
    }
}
