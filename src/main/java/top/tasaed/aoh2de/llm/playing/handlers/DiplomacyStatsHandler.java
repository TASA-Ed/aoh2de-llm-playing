package top.tasaed.aoh2de.llm.playing.handlers;

import age.of.civilizations2.jakowski.lukasz.CFG;
import com.alibaba.fastjson2.JSONObject;
import top.tasaed.aoh2de.llm.playing.HttpResponses;

public final class DiplomacyStatsHandler extends GameRequestHandler {
    public DiplomacyStatsHandler() {
        super("GET_DIPLOMACY_STATS_FAILED", "Failed to get diplomacy stats.");
    }

    @Override
    protected JSONObject handleOnGameThread(JSONObject request) {
        Integer civId;
        try {
            civId = request.getInteger("civilizationId");
        } catch (RuntimeException exception) {
            return HttpResponses.error("INVALID_PARAMETER", "civilizationId must be integers.");
        }

        if (civId == null) {
            return HttpResponses.error("MISSING_PARAMETER", "civilizationId are required.");
        }
        int playerId = CFG.core.getPlayer(CFG.PLAYER_TURN_ID).getCivId();

        JSONObject result = new JSONObject();

        result.put("our_opinion", CFG.core.getCivRelationOfCivB(playerId, civId));
        result.put("their_opinion", CFG.core.getCivRelationOfCivB(civId, playerId));

        result.put("atWar", CFG.core.getCivsAtWar(playerId, civId));
        result.put("truceTurns", CFG.core.getCivTruce(playerId, civId));
        result.put("militaryAccessTurns", CFG.core.getMilitaryAccess(playerId, civId));
        result.put("nonAggressionPactTurns", CFG.core.getCivNonAggressionPact(playerId, civId));
        result.put("defensivePactTurns", CFG.core.getDefensivePact(playerId, civId));
        result.put("guaranteeTurns", CFG.core.getGuarantee(playerId, civId));

        result.put(
                "isAllied",
                CFG.core.getCiv(playerId).getAlliance() != 0
                        && CFG.core.getCiv(playerId).getAlliance()
                                == CFG.core.getCiv(civId).getAlliance());

        return HttpResponses.success(result);
    }
}
