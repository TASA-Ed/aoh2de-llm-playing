package top.tasaed.aoh2de.llm.playing.handlers;

import age.of.civilizations2.jakowski.lukasz.CFG;
import age.of.civilizations2.jakowski.lukasz.Civilization;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;
import top.tasaed.aoh2de.llm.playing.modes.HttpResponses;

public final class NeighborCivsHandler extends GameRequestHandler {
    public NeighborCivsHandler() {
        super("GET_NEIGHBOR_CIVS_FAILED", "Failed to get the neighbor civilizations list.");
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

        if (civId == null) civId = CFG.core.getPlayer(CFG.PLAYER_TURN_ID).getCivId();

        Civilization player = CFG.core.getCiv(civId);

        player.civNeighbors.buildNeighbors(player.getCivId());

        ArrayNode neighbors = JsonNodeFactory.instance.arrayNode();
        for (int i = 0; i < player.civNeighbors.civsSize; i++) {
            int neighborCivID = player.civNeighbors.civs.get(i).civID;

            if (neighborCivID > 0 && neighborCivID < CFG.core.getCivsSize()) {
                Civilization neighborCiv = CFG.core.getCiv(neighborCivID);

                if (neighborCiv.getNumOfProvs() > 0) {
                    ObjectNode civInfo = JsonNodeFactory.instance.objectNode();
                    civInfo.put("id", neighborCivID);
                    civInfo.put("name", neighborCiv.getCivName());
                    civInfo.put("provinceCount", neighborCiv.getNumOfProvs());
                    neighbors.add(civInfo);
                }
            }
        }

        ObjectNode result = JsonNodeFactory.instance.objectNode();
        result.set("neighbors", neighbors);
        result.put("neighborCount", neighbors.size());
        return HttpResponses.success(result);
    }
}
