package top.tasaed.aoh2de.llm.playing.handlers;

import age.of.civilizations2.jakowski.lukasz.CFG;
import age.of.civilizations2.jakowski.lukasz.GameCalendar;
import age.of.civilizations2.jakowski.lukasz.Menus.Info.Menu_InGame_ProvInfo;
import com.alibaba.fastjson2.JSONObject;
import top.tasaed.aoh2de.llm.playing.HttpResponses;

public final class EndTurnHandler extends GameRequestHandler {
    public EndTurnHandler() {
        super("END_TURN_FAILED", "Failed to execute the end turn action.");
    }

    @Override
    protected JSONObject handleOnGameThread(JSONObject request) {
        if (!CFG.menus.getInGameProvInfo().getMenuElem(0).getIsClickable()) {
            return HttpResponses.error("CLICK_END_TURN_NOT_ALLOWED", "Click end turn is not allowed at this time.");
        }

        int beforeTurnId = GameCalendar.TURNID;

        Menu_InGame_ProvInfo.clickEndTurn();

        JSONObject result = new JSONObject();

        result.put("beforeTurnId", beforeTurnId);

        return HttpResponses.success(result);
    }
}
