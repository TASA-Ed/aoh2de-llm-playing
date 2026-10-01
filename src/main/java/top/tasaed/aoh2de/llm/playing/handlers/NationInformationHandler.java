package top.tasaed.aoh2de.llm.playing.handlers;

import age.of.civilizations2.jakowski.lukasz.CFG;
import age.of.civilizations2.jakowski.lukasz.Civilization;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;
import top.tasaed.aoh2de.llm.playing.modes.HttpResponses;

public final class NationInformationHandler extends GameRequestHandler {
    public NationInformationHandler() {
        super("GET_NATION_INFORMATION_FAILED", "Failed to get the nation information.");
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
        ObjectNode information = JsonNodeFactory.instance.objectNode();
        information.put("civilizationId", civ.getCivId());
        information.put("name", civ.getCivName());
        information.put("tagId", civ.getCivTag());
        information.put("capitalProvinceId", civ.getCapitalProvID());
        information.put("ideologyId", civ.getIdeology());
        information.put("religionId", civ.getReligionID());
        information.put("groupId", civ.getGroupID());
        information.put("puppetOfCivilizationId", civ.getPuppetOfCiv());
        information.put("allianceId", civ.getAlliance());
        information.put("holyRomanEmpireMember", civ.getIsPartOfHolyRomanEmpire());
        information.put("rankPosition", civ.getRankPos());
        information.put("rankScore", civ.getRankScore());
        return HttpResponses.success(information);
    }
}
