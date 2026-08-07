package top.tasaed.aoh2de.llm.playing.handlers;

import age.of.civilizations2.jakowski.lukasz.CFG;
import age.of.civilizations2.jakowski.lukasz.Civilization;
import age.of.civilizations2.jakowski.lukasz.GameManager;
import age.of.civilizations2.jakowski.lukasz.GameValues.GameValues;
import com.alibaba.fastjson2.JSONObject;
import top.tasaed.aoh2de.llm.playing.HttpResponses;

public final class ChangeRelationHandler extends GameRequestHandler {

    public ChangeRelationHandler() {
        super("CHANGE_RELATION_FAILED", "Failed to change relation.");
    }

    @Override
    protected JSONObject handleOnGameThread(JSONObject request) {
        Integer civId;
        Integer turns;
        String type;
        try {
            civId = request.getInteger("civilizationId");
            turns = request.getInteger("turns");
            type = request.getString("type");
        } catch (RuntimeException exception) {
            return HttpResponses.error(
                    "INVALID_PARAMETER", "civilizationId and turns must be integers, and type must be a string.");
        }
        if (civId == null || turns == null || type == null) {
            return HttpResponses.error("MISSING_PARAMETER", "civilizationId, type and turns are required.");
        }

        int playerId = CFG.core.getPlayer(CFG.PLAYER_TURN_ID).getCivId();
        Civilization player = CFG.core.getCiv(playerId);

        switch (type) {
            case "decrease":
                int decreaseMin = GameValues.gvRelationDecrease.SUSPEND_DIPLOMATIC_RELATIONS_MIN;
                int decreaseMax = GameValues.gvRelationDecrease.SUSPEND_DIPLOMATIC_RELATIONS_MAX;
                if (turns < decreaseMin || turns > decreaseMax) {
                    return HttpResponses.error(
                            "INVALID_TURNS",
                            "Invalid turns, it must be at least " + decreaseMin
                                    + " and no more than "
                                    + decreaseMax
                                    + ".");
                }

                int decreasePoints = GameValues.gvRelationDecrease.COST_OFFER_DECREASE_RELATIONS_DIPLOMACY_POINTS;
                if (decreasePoints > player.getDiploPoints()) {
                    return HttpResponses.error(
                            "INSUFFICIENT_DIPLOMACY_POINTS",
                            "Insufficient diplomacy points, " + decreasePoints + " required");
                }

                GameManager.decreaseRelation(playerId, civId, turns);
                break;
            case "improve":
                int max = GameValues.gvRelationImprove.IMPROVE_RELATIONS_MAX_NUM_OF_TURNS;
                if (turns < 1 || turns > max) {
                    return HttpResponses.error(
                            "INVALID_TURNS", "Invalid turns, it must be at least 1 and no more than " + max + ".");
                }

                int points = GameValues.gvRelationImprove.COST_OFFER_IMPROVE_RELATIONS_DIPLOMACY_POINTS;
                if (points > player.getDiploPoints()) {
                    return HttpResponses.error(
                            "INSUFFICIENT_DIPLOMACY_POINTS", "Insufficient diplomacy points, " + points + " required");
                }

                player.getCivDiploGD().addImproveRelations(playerId, civId, turns);

                break;
            default:
                return HttpResponses.error("UNKNOWN_TYPE", "The type must be improve or decrease");
        }

        return HttpResponses.success();
    }
}
