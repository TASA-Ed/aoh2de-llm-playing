package top.tasaed.aoh2de.llm.playing.handlers;

import age.of.civilizations2.jakowski.lukasz.CFG;
import age.of.civilizations2.jakowski.lukasz.Civilization;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;
import top.tasaed.aoh2de.llm.playing.modes.HttpResponses;

public final class ArmyListHandler extends GameRequestHandler {
    public ArmyListHandler() {
        super("GET_ARMY_LIST_FAILED", "Failed to get the army list.");
    }

    @Override
    protected ObjectNode handleOnGameThread(ObjectNode request) {
        int civID = CFG.core.getPlayer(CFG.PLAYER_TURN_ID).getCivId();
        Civilization player = CFG.core.getCiv(civID);
        ArrayNode armyList = JsonNodeFactory.instance.arrayNode();

        for (int i = 0; i < player.getNumOfProvs(); i++) {
            int provinceID = player.getProvID(i);
            if (CFG.core.getProv(provinceID).getArmyCivID1(civID) > 0) {
                ObjectNode army = JsonNodeFactory.instance.objectNode();
                army.put("provinceId", provinceID);
                army.put("troops", CFG.core.getProv(provinceID).getArmyCivID1(civID));
                armyList.add(army);
            }
        }

        for (int i = 0; i < player.getArmyInAnotherProvinceSize(); i++) {
            int provinceID = player.getArmyInAnotherProviP(i);
            if (CFG.core.getProv(provinceID).getArmyCivID1(civID) > 0) {
                ObjectNode army = JsonNodeFactory.instance.objectNode();
                army.put("provinceId", provinceID);
                army.put("troops", CFG.core.getProv(provinceID).getArmyCivID1(civID));
                armyList.add(army);
            }
        }

        ObjectNode result = JsonNodeFactory.instance.objectNode();
        result.set("armyList", armyList);
        return HttpResponses.success(result);
    }
}
