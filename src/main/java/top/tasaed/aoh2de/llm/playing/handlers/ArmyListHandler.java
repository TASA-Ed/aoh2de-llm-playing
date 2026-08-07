package top.tasaed.aoh2de.llm.playing.handlers;

import age.of.civilizations2.jakowski.lukasz.CFG;
import age.of.civilizations2.jakowski.lukasz.Civilization;
import com.alibaba.fastjson2.JSONObject;
import java.util.ArrayList;
import java.util.List;
import top.tasaed.aoh2de.llm.playing.HttpResponses;

public final class ArmyListHandler extends GameRequestHandler {
    public ArmyListHandler() {
        super("GET_ARMY_LIST_FAILED", "Failed to get the army list.");
    }

    @Override
    protected JSONObject handleOnGameThread(JSONObject request) {
        int civID = CFG.core.getPlayer(CFG.PLAYER_TURN_ID).getCivId();
        Civilization player = CFG.core.getCiv(civID);
        List<JSONObject> armyList = new ArrayList<>();

        for (int i = 0; i < player.getNumOfProvs(); i++) {
            int provinceID = player.getProvID(i);
            if (CFG.core.getProv(provinceID).getArmyCivID1(civID) > 0) {
                JSONObject army = new JSONObject();
                army.put("provinceId", provinceID);
                army.put("troops", CFG.core.getProv(provinceID).getArmyCivID1(civID));
                armyList.add(army);
            }
        }

        for (int i = 0; i < player.getArmyInAnotherProvinceSize(); i++) {
            int provinceID = player.getArmyInAnotherProviP(i);
            if (CFG.core.getProv(provinceID).getArmyCivID1(civID) > 0) {
                JSONObject army = new JSONObject();
                army.put("provinceId", provinceID);
                army.put("troops", CFG.core.getProv(provinceID).getArmyCivID1(civID));
                armyList.add(army);
            }
        }

        JSONObject result = new JSONObject();
        result.put("armyList", armyList);
        return HttpResponses.success(result);
    }
}
