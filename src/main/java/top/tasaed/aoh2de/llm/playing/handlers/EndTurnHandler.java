package top.tasaed.aoh2de.llm.playing.handlers;

import top.tasaed.aoh2de.llm.playing.HttpResponses;

import age.of.civilizations2.jakowski.lukasz.Menus.Info.Menu_InGame_ProvInfo;
import com.alibaba.fastjson2.JSONObject;

public final class EndTurnHandler extends GameRequestHandler {
    public EndTurnHandler() {
        super("END_TURN_FAILED", "Failed to execute the end turn action.");
    }

    @Override
    protected JSONObject handleOnGameThread(JSONObject request) {
        Menu_InGame_ProvInfo.clickEndTurn();
        return HttpResponses.success();
    }
}
