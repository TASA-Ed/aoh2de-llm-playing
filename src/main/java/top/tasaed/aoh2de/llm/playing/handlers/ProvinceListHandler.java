package top.tasaed.aoh2de.llm.playing.handlers;

import age.of.civilizations2.jakowski.lukasz.CFG;
import age.of.civilizations2.jakowski.lukasz.Civilization;
import age.of.civilizations2.jakowski.lukasz.Province;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;
import top.tasaed.aoh2de.llm.playing.modes.HttpResponses;

public final class ProvinceListHandler extends GameRequestHandler {
    public ProvinceListHandler() {
        super("GET_PROVINCE_LIST_FAILED", "Failed to get the province list.");
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

        Civilization civ = CFG.core.getCiv(civId);
        ArrayNode provinces = JsonNodeFactory.instance.arrayNode();
        for (int i = 0; i < civ.getNumOfProvs(); i++) {
            Province province = CFG.core.getProv(civ.getProvID(i));
            ObjectNode information = JsonNodeFactory.instance.objectNode();
            information.put("name", province.getName());
            information.put("id", province.getProvID());
            provinces.add(information);
        }

        ObjectNode result = JsonNodeFactory.instance.objectNode();
        result.set("provinces", provinces);
        return HttpResponses.success(result);
    }
}
