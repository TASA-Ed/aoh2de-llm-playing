package top.tasaed.aoh2de.llm.playing.handlers;

import age.of.civilizations2.jakowski.lukasz.CFG;
import age.of.civilizations2.jakowski.lukasz.Civilization;
import age.of.civilizations2.jakowski.lukasz.Core.Core;
import age.of.civilizations2.jakowski.lukasz.MapA.Plagues.Nuke.NukeManager;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import top.tasaed.aoh2de.llm.playing.HttpResponses;

public final class CivilizationViewHandler extends GameRequestHandler {
    public CivilizationViewHandler() {
        super("GET_CIVILIZATION_VIEW_FAILED", "Failed to get the civilization view information.");
    }

    @Override
    protected JSONObject handleOnGameThread(JSONObject request) {
        int civId = CFG.core.getPlayer(CFG.PLAYER_TURN_ID).getCivId();

        Civilization civ = CFG.core.getCiv(civId);
        JSONObject viewData = new JSONObject();

        // 基本信息
        JSONObject basicInfo = new JSONObject();
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

            JSONArray allianceMembers = new JSONArray();
            for (int i = 0; i < CFG.core.getAlliance(civ.getAlliance()).getCivilizationsSize(); i++) {
                int memberCivId = CFG.core.getAlliance(civ.getAlliance()).getCivilization(i);
                JSONObject member = new JSONObject();
                member.put("civilizationId", memberCivId);
                member.put("name", CFG.core.getCiv(memberCivId).getCivName());
                allianceMembers.add(member);
            }
            basicInfo.put("allianceMembers", allianceMembers);
        }

        viewData.put("basicInfo", basicInfo);

        // 领土信息
        JSONObject territoryInfo = new JSONObject();
        territoryInfo.put("numberOfProvinces", civ.getNumOfProvs());

        // 按地形类型统计省份
        JSONArray provincesByTerrain = new JSONArray();
        java.util.Map<Integer, Integer> terrainCount = new java.util.HashMap<>();

        for (int i = 0; i < civ.getNumOfProvs(); i++) {
            int provId = civ.getProvID(i);
            int terrainType = CFG.core.getProv(provId).getTerrainTypeID();
            terrainCount.put(terrainType, terrainCount.getOrDefault(terrainType, 0) + 1);
        }

        for (java.util.Map.Entry<Integer, Integer> entry : terrainCount.entrySet()) {
            JSONObject terrain = new JSONObject();
            terrain.put("terrainTypeId", entry.getKey());
            terrain.put("terrainName", CFG.terrainTypesManager.getName(entry.getKey()));
            terrain.put("provinceCount", entry.getValue());
            provincesByTerrain.add(terrain);
        }

        territoryInfo.put("provincesByTerrain", provincesByTerrain);
        viewData.put("territoryInfo", territoryInfo);

        // 人口信息
        JSONObject populationInfo = new JSONObject();
        populationInfo.put("totalPopulation", civ.countPop());
        viewData.put("populationInfo", populationInfo);

        // 军事信息
        JSONObject militaryInfo = new JSONObject();
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
        viewData.put("militaryInfo", militaryInfo);

        // 经济信息
        JSONObject economyInfo = new JSONObject();
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

        viewData.put("economyInfo", economyInfo);

        // 科技信息
        JSONObject technologyInfo = new JSONObject();
        technologyInfo.put("technologyLevel", ((int) (civ.getTechLevel() * 100.0f)) / 100.0f);
        viewData.put("technologyInfo", technologyInfo);

        // 发展信息
        JSONObject developmentInfo = new JSONObject();
        developmentInfo.put("averageDevelopment", CFG.core.countAverageDevelopmentLevel(civId));
        developmentInfo.put("averageDevelopmentFloat", CFG.core.countAverageDevelopmentLevel_Float(civId));

        if (civ.getTechLevel() > 0) {
            developmentInfo.put("developmentPercentageOfTech", (int)
                    ((CFG.core.countAverageDevelopmentLevel_Float(civId) / civ.getTechLevel()) * 100.0f));
        } else {
            developmentInfo.put("developmentPercentageOfTech", 0);
        }

        viewData.put("developmentInfo", developmentInfo);

        // 通货膨胀信息
        JSONObject inflationInfo = new JSONObject();
        inflationInfo.put("inflationCost", (int) CFG.gameUpdate.getInflation(civId));
        inflationInfo.put("inflationPercentage", ((int) (CFG.gameUpdate.getInflationPerc(civId) * 10000.0f)) / 100.0f);
        viewData.put("inflationInfo", inflationInfo);

        // 核武器信息
        JSONObject nukesInfo = new JSONObject();
        nukesInfo.put("numberOfNukes", civ.civGD.iNukes);
        nukesInfo.put("nukesLimit", NukeManager.getAtomicBombsLimit(civId));
        viewData.put("nukesInfo", nukesInfo);

        // 幸福度信息
        JSONObject happinessInfo = new JSONObject();
        happinessInfo.put("happiness", civ.getHappiness());
        viewData.put("happinessInfo", happinessInfo);

        // 稳定度信息
        JSONObject stabilityInfo = new JSONObject();
        stabilityInfo.put("stability", (int) (civ.getStabilityCiv() * 100.0f));
        viewData.put("stabilityInfo", stabilityInfo);

        // 排名信息
        JSONObject rankInfo = new JSONObject();
        rankInfo.put("rankPosition", civ.getRankPos());
        rankInfo.put("rankScore", civ.getRankScore());
        viewData.put("rankInfo", rankInfo);

        // 制裁信息
        JSONObject sanctionsInfo = new JSONObject();
        sanctionsInfo.put("sanctionsImpact", (int) (civ.sanctionsImpact * 10000.0f) / 100.0f);
        viewData.put("sanctionsInfo", sanctionsInfo);

        // 政府（意识形态）信息
        JSONObject governmentInfo = new JSONObject();
        governmentInfo.put("ideologyId", civ.getIdeology());
        governmentInfo.put(
                "ideologyName",
                CFG.ideologiesMgr.getIdeologyID(civ.getIdeology()).getName());
        viewData.put("governmentInfo", governmentInfo);

        // 宗教信息
        JSONObject religionInfo = new JSONObject();
        religionInfo.put("religionId", civ.getReligionID());
        religionInfo.put(
                "religionName",
                CFG.religionManager.getReligion(civ.getReligionID()).getName());
        viewData.put("religionInfo", religionInfo);

        // 游戏设置信息
        JSONObject gameSettingsInfo = new JSONObject();
        gameSettingsInfo.put("difficulty", CFG.DIFFICULTY);
        gameSettingsInfo.put("difficultyName", CFG.getDifficultyName(CFG.DIFFICULTY));
        gameSettingsInfo.put("armyRetreatThreshold", (int) (CFG.ARMY_RETREAT * 100.0f));
        gameSettingsInfo.put("capitulationThreshold", (int) (CFG.CAPITULATION * 100.0f));
        viewData.put("gameSettingsInfo", gameSettingsInfo);

        return HttpResponses.success(viewData);
    }
}
