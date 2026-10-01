package top.tasaed.aoh2de.llm.playing.handlers;

import age.of.civilizations2.jakowski.lukasz.CFG;
import tools.jackson.databind.node.ObjectNode;
import top.tasaed.aoh2de.llm.playing.modes.HttpResponses;

public final class DeclareWarHandler extends GameRequestHandler {
    public DeclareWarHandler() {
        super("DECLARE_WAR_FAILED", "Failure to Declare War.");
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
            return HttpResponses.error("MISSING_PARAMETER", "civilizationId is required.");
        }

        if (CFG.core.getCiv(civId).getNumOfProvs() <= 0) {
            return HttpResponses.error("INVALID_CIVILIZATION_ID", "Civilization does not exist or has perished.");
        }

        int playerId = CFG.core.getPlayer(CFG.PLAYER_TURN_ID).getCivId();

        if (CFG.core.getCivsAtWar(playerId, civId)) {
            return HttpResponses.error("ALREADY_AT_WAR", "Already at war with this civilization, cannot declare war.");
        }

        if (CFG.core.getCivTruce(playerId, civId) > 0) {
            return HttpResponses.error(
                    "CEASEFIRE_IN_PROGRESS", "Currently in a ceasefire with this civilization, cannot declare war.");
        }

        CFG.core.declareWar(playerId, civId, false);

        return HttpResponses.success();
    }
}
