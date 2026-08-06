package top.tasaed.aoh2de.llm.playing.handlers;

import top.tasaed.aoh2de.llm.playing.HttpResponses;

import age.of.civilizations2.jakowski.lukasz.CFG;
import age.of.civilizations2.jakowski.lukasz.Civilization;
import age.of.civilizations2.jakowski.lukasz.Province;
import com.alibaba.fastjson2.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class ProvinceListHandler extends GameRequestHandler {
    public ProvinceListHandler() {
        super("GET_PROVINCE_LIST_FAILED", "Failed to get the province list.");
    }

    @Override
    protected JSONObject handleOnGameThread(JSONObject request) {
        Civilization player = CFG.core.getCiv(CFG.core.getPlayer(CFG.PLAYER_TURN_ID).getCivId());
        List<JSONObject> provinces = new ArrayList<>();
        for (int i = 0; i < player.getNumOfProvs(); i++) {
            Province province = CFG.core.getProv(player.getProvID(i));
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
