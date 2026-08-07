package top.tasaed.aoh2de.llm.playing.handlers;

import age.of.civilizations2.jakowski.lukasz.CFG;
import age.of.civilizations2.jakowski.lukasz.GameAction;
import age.of.civilizations2.jakowski.lukasz.GameCalendar;
import com.alibaba.fastjson2.JSONObject;
import top.tasaed.aoh2de.llm.playing.HttpResponses;

public final class TurnStatsHandler extends GameRequestHandler {
    public TurnStatsHandler() {
        super("GET_TURN_STATS_FAILED", "Failed to get turn stats.");
    }

    @Override
    protected JSONObject handleOnGameThread(JSONObject request) {
        JSONObject result = new JSONObject();

        GameAction.TurnStates state = CFG.gameAction.getActiveTurnStateID();

        boolean clickable = CFG.menus.getInGameProvInfo().getMenuElem(0).getIsClickable();

        result.put("turnId", GameCalendar.TURNID);
        result.put("turnState", state.name());
        result.put("buttonClickable", clickable);
        result.put("hasCurrentMilitaryAction", CFG.gameAction.getCurrentMoveunits() != null);
        result.put("nextEventAvailable", state == GameAction.TurnStates.TURN_ACTIONS && clickable);
        result.put("newTurnReady", state == GameAction.TurnStates.INPUT_ORDERS && clickable);

        return HttpResponses.success(result);
    }
}
