package top.tasaed.aoh2de.llm.playing.handlers;

import top.tasaed.aoh2de.llm.playing.HttpResponses;

import age.of.civilizations2.jakowski.lukasz.CFG;
import age.of.civilizations2.jakowski.lukasz.Civilization;
import com.alibaba.fastjson2.JSONObject;

public final class NationInformationHandler extends GameRequestHandler {
    public NationInformationHandler() {
        super("GET_NATION_INFORMATION_FAILED", "Failed to get the nation information.");
    }

    @Override
    protected JSONObject handleOnGameThread(JSONObject request) {
        Civilization player = CFG.core.getCiv(CFG.core.getPlayer(CFG.PLAYER_TURN_ID).getCivId());
        JSONObject information = new JSONObject();
        information.put("civilizationId", player.getCivId());
        information.put("name", player.getCivName());
        information.put("tagId", player.getCivTag());
        information.put("capitalProvinceId", player.getCapitalProvID());
        information.put("ideologyId", player.getIdeology());
        information.put("religionId", player.getReligionID());
        information.put("groupId", player.getGroupID());
        information.put("puppetOfCivilizationId", player.getPuppetOfCiv());
        information.put("allianceId", player.getAlliance());
        information.put("holyRomanEmpireMember", player.getIsPartOfHolyRomanEmpire());
        information.put("rankPosition", player.getRankPos());
        information.put("rankScore", player.getRankScore());
        return HttpResponses.success(information);
    }
}
