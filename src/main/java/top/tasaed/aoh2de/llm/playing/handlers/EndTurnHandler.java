package top.tasaed.aoh2de.llm.playing.handlers;

import age.of.civilizations2.jakowski.lukasz.CFG;
import age.of.civilizations2.jakowski.lukasz.GameCalendar;
import age.of.civilizations2.jakowski.lukasz.Menus.Info.Menu_InGame_ProvInfo;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;
import top.tasaed.aoh2de.llm.playing.modes.HttpResponses;

public final class EndTurnHandler extends GameRequestHandler {
    public EndTurnHandler() {
        super("END_TURN_FAILED", "Failed to execute the end turn action.");
    }

    @Override
    protected ObjectNode handleOnGameThread(ObjectNode request) {
        if (!CFG.menus.getInGameProvInfo().getMenuElem(0).getIsClickable()) {
            return HttpResponses.error("CLICK_END_TURN_NOT_ALLOWED", "Click end turn is not allowed at this time.");
        }

        int beforeTurnId = GameCalendar.TURNID;

        Menu_InGame_ProvInfo.clickEndTurn();

        ObjectNode result = JsonNodeFactory.instance.objectNode();

        result.put("beforeTurnId", beforeTurnId);

        return HttpResponses.success(result);
    }
}
