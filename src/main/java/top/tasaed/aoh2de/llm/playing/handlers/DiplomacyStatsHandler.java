package top.tasaed.aoh2de.llm.playing.handlers;

import age.of.civilizations2.jakowski.lukasz.CFG;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;
import top.tasaed.aoh2de.llm.playing.modes.HttpResponses;

public final class DiplomacyStatsHandler extends GameRequestHandler {
    public DiplomacyStatsHandler() {
        super("GET_DIPLOMACY_STATS_FAILED", "Failed to get diplomacy stats.");
    }

    @Override
    protected ObjectNode handleOnGameThread(ObjectNode request) {
        Integer civId;
        try {
            civId = request.hasNonNull("civilizationId")
                    ? request.get("civilizationId").asInt()
                    : null;
        } catch (RuntimeException exception) {
            return HttpResponses.error("INVALID_PARAMETER", "civilizationId must be integers.");
        }

        if (civId == null) {
            return HttpResponses.error("MISSING_PARAMETER", "civilizationId are required.");
        }
        int playerId = CFG.core.getPlayer(CFG.PLAYER_TURN_ID).getCivId();

        ObjectNode result = JsonNodeFactory.instance.objectNode();

        result.put("our_opinion", CFG.core.getCivRelationOfCivB(playerId, civId));
        result.put("their_opinion", CFG.core.getCivRelationOfCivB(civId, playerId));

        result.put("atWar", CFG.core.getCivsAtWar(playerId, civId));
        result.put("truceTurns", CFG.core.getCivTruce(playerId, civId));
        result.put("militaryAccessTurns", CFG.core.getMilitaryAccess(playerId, civId));
        result.put("nonAggressionPactTurns", CFG.core.getCivNonAggressionPact(playerId, civId));
        result.put("defensivePactTurns", CFG.core.getDefensivePact(playerId, civId));
        result.put("guaranteeTurns", CFG.core.getGuarantee(playerId, civId));
        result.put(
                "diplomaticRelationsSuspended",
                CFG.core.getCiv(civId).getCivDiploGD().getIsEmbassyClosed(playerId));
        result.put(
                "diplomaticRelationsSuspendedTurns",
                CFG.core.getCiv(civId).getCivDiploGD().isEmbassyClosed_Turns(playerId));

        result.put(
                "improvingRelationsWith",
                CFG.core.getCiv(playerId).getCivDiploGD().getIsImprovingRelations(civId));
        result.put(
                "improvingRelationsWithTurns",
                CFG.core.getCiv(playerId).getCivDiploGD().getIsImprovingRelationsTurns(civId));
        result.put(
                "improvingRelationsFrom", CFG.core.getCiv(civId).getCivDiploGD().getIsImprovingRelations(playerId));
        result.put(
                "improvingRelationsFromTurns",
                CFG.core.getCiv(civId).getCivDiploGD().getIsImprovingRelationsTurns(playerId));

        result.put(
                "isAllied",
                CFG.core.getCiv(playerId).getAlliance() != 0
                        && CFG.core.getCiv(playerId).getAlliance()
                                == CFG.core.getCiv(civId).getAlliance());

        return HttpResponses.success(result);
    }
}
