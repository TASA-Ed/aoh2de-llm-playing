package top.tasaed.aoh2de.llm.playing.handlers;

import age.of.civilizations2.jakowski.lukasz.CFG;
import age.of.civilizations2.jakowski.lukasz.Civilization;
import age.of.civilizations2.jakowski.lukasz.Core.Core;
import age.of.civilizations2.jakowski.lukasz.MapA.Plagues.Nuke.NukeManager;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;
import top.tasaed.aoh2de.llm.playing.modes.HttpResponses;

public final class CivilizationViewHandler extends GameRequestHandler {
    public CivilizationViewHandler() {
        super("GET_CIVILIZATION_VIEW_FAILED", "Failed to get the civilization view information.");
    }

    @Override
    protected ObjectNode handleOnGameThread(ObjectNode request) {
        int civId = CFG.core.getPlayer(CFG.PLAYER_TURN_ID).getCivId();

        Civilization civ = CFG.core.getCiv(civId);
        ObjectNode viewData = JsonNodeFactory.instance.objectNode();

        // 基本信息
        ObjectNode basicInfo = JsonNodeFactory.instance.objectNode();
        basicInfo.put("civilizationId", civ.getCivId());
        basicInfo.put("name", civ.getCivName());
        basicInfo.put("capitalProvinceId", civ.getCapitalProvID());

        // 首都名称
        if (civ.getCapitalProvID() >= 0) {
            if (CFG.core.getProv(civ.getCapitalProvID()).getCitiesSize() > 0) {
                basicInfo.put(
                        "capitalName",
                        CFG.core.getProv(civ.getCapitalProvID()).getCit(0).getCityName());
            } else if (CFG.core.getProv(civ.getCapitalProvID()).getName().length() > 0) {
                basicInfo.put(
                        "capitalName", CFG.core.getProv(civ.getCapitalProvID()).getName());
            }
        }

        // 联盟信息
        if (civ.getAlliance() > 0) {
            basicInfo.put("allianceId", civ.getAlliance());
            basicInfo.put(
                    "allianceName", CFG.core.getAlliance(civ.getAlliance()).getAllianceName());

            ArrayNode allianceMembers = JsonNodeFactory.instance.arrayNode();
            for (int i = 0; i < CFG.core.getAlliance(civ.getAlliance()).getCivilizationsSize(); i++) {
                int memberCivId = CFG.core.getAlliance(civ.getAlliance()).getCivilization(i);
                ObjectNode member = JsonNodeFactory.instance.objectNode();
                member.put("civilizationId", memberCivId);
                member.put("name", CFG.core.getCiv(memberCivId).getCivName());
                allianceMembers.add(member);
            }
            basicInfo.set("allianceMembers", allianceMembers);
        }

        viewData.set("basicInfo", basicInfo);

        // 领土信息
        ObjectNode territoryInfo = JsonNodeFactory.instance.objectNode();
        territoryInfo.put("numberOfProvinces", civ.getNumOfProvs());

        // 按地形类型统计省份
        ArrayNode provincesByTerrain = JsonNodeFactory.instance.arrayNode();
        java.util.Map<Integer, Integer> terrainCount = new java.util.HashMap<>();

        for (int i = 0; i < civ.getNumOfProvs(); i++) {
            int provId = civ.getProvID(i);
            int terrainType = CFG.core.getProv(provId).getTerrainTypeID();
            terrainCount.put(terrainType, terrainCount.getOrDefault(terrainType, 0) + 1);
        }

        for (java.util.Map.Entry<Integer, Integer> entry : terrainCount.entrySet()) {
            ObjectNode terrain = JsonNodeFactory.instance.objectNode();
            terrain.put("terrainTypeId", entry.getKey());
            terrain.put("terrainName", CFG.terrainTypesManager.getName(entry.getKey()));
            terrain.put("provinceCount", entry.getValue());
            provincesByTerrain.add(terrain);
        }

        territoryInfo.set("provincesByTerrain", provincesByTerrain);
        viewData.set("territoryInfo", territoryInfo);

        // 人口信息
        ObjectNode populationInfo = JsonNodeFactory.instance.objectNode();
        populationInfo.put("totalPopulation", civ.countPop());
        viewData.set("populationInfo", populationInfo);

        // 军事信息
        ObjectNode militaryInfo = JsonNodeFactory.instance.objectNode();
        militaryInfo.put("numberOfUnits", civ.getNumberOfUnits());
        militaryInfo.put("militaryUpkeep", (int) CFG.gameUpdate.getMilitaryUpkeep_Total(civId));

        if (civ.getNumberOfUnits() > 0) {
            militaryInfo.put(
                    "upkeepPerUnit",
                    (int) ((CFG.gameUpdate.getMilitaryUpkeep_Total(civId) / civ.getNumberOfUnits()) * 100.0f) / 100.0f);
        } else {
            militaryInfo.put("upkeepPerUnit", 0.0f);
        }

        militaryInfo.put("warWeariness", ((int) (civ.getWarWeariness() * 10000.0f)) / 100.0f);
        viewData.set("militaryInfo", militaryInfo);

        // 经济信息
        ObjectNode economyInfo = JsonNodeFactory.instance.objectNode();
        long totalEconomy = civ.countEco();
        economyInfo.put("totalEconomy", totalEconomy);
        economyInfo.put("startingEconomy", civ.civGD.startingEconomy);
        economyInfo.put("economyDifference", totalEconomy - civ.civGD.startingEconomy);
        economyInfo.put("overinvestmentPenalty", (int) (Core.getOverInvestmentsPenalty(civId) * 10000.0f) / 100.0f);
        economyInfo.put("unemploymentPopulation", CFG.gameUpdate.getUnemploymentPop(civId));

        if (civ.countPop() > 0) {
            economyInfo.put(
                    "unemploymentPercentage",
                    (int) ((CFG.gameUpdate.getUnemploymentPop(civId) / (float) civ.countPop()) * 10000.0f) / 100.0f);
        } else {
            economyInfo.put("unemploymentPercentage", 0.0f);
        }

        viewData.set("economyInfo", economyInfo);

        // 科技信息
        ObjectNode technologyInfo = JsonNodeFactory.instance.objectNode();
        technologyInfo.put("technologyLevel", ((int) (civ.getTechLevel() * 100.0f)) / 100.0f);
        viewData.set("technologyInfo", technologyInfo);

        // 发展信息
        ObjectNode developmentInfo = JsonNodeFactory.instance.objectNode();
        developmentInfo.put("averageDevelopment", CFG.core.countAverageDevelopmentLevel(civId));
        developmentInfo.put("averageDevelopmentFloat", CFG.core.countAverageDevelopmentLevel_Float(civId));

        if (civ.getTechLevel() > 0) {
            developmentInfo.put("developmentPercentageOfTech", (int)
                    ((CFG.core.countAverageDevelopmentLevel_Float(civId) / civ.getTechLevel()) * 100.0f));
        } else {
            developmentInfo.put("developmentPercentageOfTech", 0);
        }

        viewData.set("developmentInfo", developmentInfo);

        // 通货膨胀信息
        ObjectNode inflationInfo = JsonNodeFactory.instance.objectNode();
        inflationInfo.put("inflationCost", (int) CFG.gameUpdate.getInflation(civId));
        inflationInfo.put("inflationPercentage", ((int) (CFG.gameUpdate.getInflationPerc(civId) * 10000.0f)) / 100.0f);
        viewData.set("inflationInfo", inflationInfo);

        // 核武器信息
        ObjectNode nukesInfo = JsonNodeFactory.instance.objectNode();
        nukesInfo.put("numberOfNukes", civ.civGD.iNukes);
        nukesInfo.put("nukesLimit", NukeManager.getAtomicBombsLimit(civId));
        viewData.set("nukesInfo", nukesInfo);

        // 幸福度信息
        ObjectNode happinessInfo = JsonNodeFactory.instance.objectNode();
        happinessInfo.put("happiness", civ.getHappiness());
        viewData.set("happinessInfo", happinessInfo);

        // 稳定度信息
        ObjectNode stabilityInfo = JsonNodeFactory.instance.objectNode();
        stabilityInfo.put("stability", (int) (civ.getStabilityCiv() * 100.0f));
        viewData.set("stabilityInfo", stabilityInfo);

        // 排名信息
        ObjectNode rankInfo = JsonNodeFactory.instance.objectNode();
        rankInfo.put("rankPosition", civ.getRankPos());
        rankInfo.put("rankScore", civ.getRankScore());
        viewData.set("rankInfo", rankInfo);

        // 制裁信息
        ObjectNode sanctionsInfo = JsonNodeFactory.instance.objectNode();
        sanctionsInfo.put("sanctionsImpact", (int) (civ.sanctionsImpact * 10000.0f) / 100.0f);
        viewData.set("sanctionsInfo", sanctionsInfo);

        // 政府（意识形态）信息
        ObjectNode governmentInfo = JsonNodeFactory.instance.objectNode();
        governmentInfo.put("ideologyId", civ.getIdeology());
        governmentInfo.put(
                "ideologyName",
                CFG.ideologiesMgr.getIdeologyID(civ.getIdeology()).getName());
        viewData.set("governmentInfo", governmentInfo);

        // 宗教信息
        ObjectNode religionInfo = JsonNodeFactory.instance.objectNode();
        religionInfo.put("religionId", civ.getReligionID());
        religionInfo.put(
                "religionName",
                CFG.religionManager.getReligion(civ.getReligionID()).getName());
        viewData.set("religionInfo", religionInfo);

        // 游戏设置信息
        ObjectNode gameSettingsInfo = JsonNodeFactory.instance.objectNode();
        gameSettingsInfo.put("difficulty", CFG.DIFFICULTY);
        gameSettingsInfo.put("difficultyName", CFG.getDifficultyName(CFG.DIFFICULTY));
        gameSettingsInfo.put("armyRetreatThreshold", (int) (CFG.ARMY_RETREAT * 100.0f));
        gameSettingsInfo.put("capitulationThreshold", (int) (CFG.CAPITULATION * 100.0f));
        viewData.set("gameSettingsInfo", gameSettingsInfo);

        return HttpResponses.success(viewData);
    }
}
