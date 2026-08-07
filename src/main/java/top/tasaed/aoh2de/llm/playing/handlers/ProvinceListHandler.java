package top.tasaed.aoh2de.llm.playing.handlers;

import age.of.civilizations2.jakowski.lukasz.CFG;
import age.of.civilizations2.jakowski.lukasz.Civilization;
import age.of.civilizations2.jakowski.lukasz.Province;
import com.alibaba.fastjson2.JSONObject;
import java.util.ArrayList;
import java.util.List;
import top.tasaed.aoh2de.llm.playing.HttpResponses;

public final class ProvinceListHandler extends GameRequestHandler {
    public ProvinceListHandler() {
        super("GET_PROVINCE_LIST_FAILED", "Failed to get the province list.");
    }

    @Override
    protected JSONObject handleOnGameThread(JSONObject request) {
        Integer civId;

        try {
            civId = request.getInteger("civilizationId");
        } catch (RuntimeException exception) {
            return HttpResponses.error("INVALID_PARAMETER", "civilizationId must be integers.");
        }

        if (civId == null) civId = CFG.core.getPlayer(CFG.PLAYER_TURN_ID).getCivId();

        Civilization civ = CFG.core.getCiv(civId);
        List<JSONObject> provinces = new ArrayList<>();
        for (int i = 0; i < civ.getNumOfProvs(); i++) {
            Province province = CFG.core.getProv(civ.getProvID(i));
            JSONObject information = new JSONObject();
            information.put("name", province.getName());
            information.put("id", province.getProvID());
            provinces.add(information);
        }

        JSONObject result = new JSONObject();
        result.put("provinces", provinces);
        return HttpResponses.success(result);
    }
}
