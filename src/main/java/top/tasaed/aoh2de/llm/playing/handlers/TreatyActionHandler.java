package top.tasaed.aoh2de.llm.playing.handlers;

import age.of.civilizations2.jakowski.lukasz.CFG;
import age.of.civilizations2.jakowski.lukasz.Civilization;
import age.of.civilizations2.jakowski.lukasz.GameManager;
import age.of.civilizations2.jakowski.lukasz.GameValues.GameValues;
import com.alibaba.fastjson2.JSONObject;
import top.tasaed.aoh2de.llm.playing.modes.HttpResponses;

public final class TreatyActionHandler extends GameRequestHandler {
    public TreatyActionHandler() {
        super("TREATY_ACTION_FAILED", "Failed to send treaty proposal.");
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

        int maxTurns;
        int cost;
        switch (type) {
            case "ask_military_access":
                maxTurns = GameValues.gvDipMilitaryAccess.DIPLOMACY_MAX_NUMBER_OF_TURNS_FOR_MILITARY_ACCESS;
                cost = GameValues.gvDipMilitaryAccess.COST_OFFER_MILITARY_ACCESS_ASK_DIPLOMACY_POINTS;
                break;
            case "offer_military_access":
                maxTurns = GameValues.gvDipMilitaryAccess.DIPLOMACY_MAX_NUMBER_OF_TURNS_FOR_MILITARY_ACCESS;
                cost = GameValues.gvDipMilitaryAccess.COST_OFFER_MILITARY_ACCESS_GIVE_DIPLOMACY_POINTS;
                break;
            case "defensive_pact":
                maxTurns = GameValues.gvDipDefensivePact.DIPLOMACY_MAX_NUMBER_OF_TURNS_FOR_DEFENSIVE_PACT;
                cost = GameValues.gvDipDefensivePact.COST_OFFER_DEFENSIVE_PACT_DIPLOMACY_POINTS;
                break;
            case "non_aggression_pact":
                maxTurns = GameValues.gvDipNonAggression.DIPLOMACY_MAX_NUMBER_OF_TURNS_NON_AGGRESSION_PACT;
                cost = GameValues.gvDipNonAggression.COST_OFFER_NONAGGRESSION_PACT_DIPLOMACY_POINTS;
                break;
            case "guarantee_independence":
                maxTurns = GameValues.gvDipGuarantee.DIPLOMACY_MAX_NUMBER_OF_TURNS_FOR_GUARANTEE;
                // The sender charges military-access ask cost, not the guarantee menu's displayed cost.
                cost = GameValues.gvDipMilitaryAccess.COST_OFFER_MILITARY_ACCESS_ASK_DIPLOMACY_POINTS;
                break;
            default:
                return HttpResponses.error(
                        "UNKNOWN_TYPE",
                        "The type must be ask_military_access, offer_military_access, defensive_pact, non_aggression_pact or guarantee_independence.");
        }
        if (turns < 1 || turns > maxTurns) {
            return HttpResponses.error("INVALID_TURNS", "turns must be between 1 and " + maxTurns + ".");
        }
        int playerId = CFG.core.getPlayer(CFG.PLAYER_TURN_ID).getCivId();
        if (civId <= 0
                || civId >= CFG.core.getCivsSize()
                || civId == playerId
                || CFG.core.getCiv(civId).getNumOfProvs() <= 0) {
            return HttpResponses.error("INVALID_CIVILIZATION_ID", "Target must be another living civilization.");
        }
        Civilization player = CFG.core.getCiv(playerId);
        if (player.getDiploPoints() < cost) {
            return HttpResponses.error(
                    "INSUFFICIENT_DIPLOMACY_POINTS", "Insufficient diplomacy points, " + cost + " required.");
        }
        switch (type) {
            case "ask_military_access":
                GameManager.sendMilitaryAccess_AskProposal(civId, playerId, turns);
                break;
            case "offer_military_access":
                GameManager.sendMilitaryAccess_GiveProposal(civId, playerId, turns);
                break;
            case "defensive_pact":
                GameManager.sendDefensivePactProposal(civId, playerId, turns);
                break;
            case "non_aggression_pact":
                GameManager.sendNonAggressionProposal(civId, playerId, turns);
                break;
            case "guarantee_independence":
                GameManager.sendGuaranteeIndependence_AskProposal(civId, playerId, turns);
                break;
        }
        return HttpResponses.success();
    }
}
