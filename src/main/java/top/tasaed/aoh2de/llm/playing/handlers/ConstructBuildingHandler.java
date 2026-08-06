package top.tasaed.aoh2de.llm.playing.handlers;

import age.of.civilizations2.jakowski.lukasz.CFG;
import age.of.civilizations2.jakowski.lukasz.Civilization;
import age.of.civilizations2.jakowski.lukasz.Civilizations.Construction.ConstructionType;
import age.of.civilizations2.jakowski.lukasz.GameAction;
import age.of.civilizations2.jakowski.lukasz.MapA.BuildingsManager;
import age.of.civilizations2.jakowski.lukasz.Province;
import com.alibaba.fastjson2.JSONObject;
import java.util.Locale;
import top.tasaed.aoh2de.llm.playing.HttpResponses;

public final class ConstructBuildingHandler extends GameRequestHandler {

    public ConstructBuildingHandler() {
        super("CONSTRUCT_BUILDING_FAILED", "Failed to construct the building.");
    }

    @Override
    protected JSONObject handleOnGameThread(JSONObject request) {
        Integer provinceId;
        String buildingType;

        try {
            provinceId = request.getInteger("provinceId");
            buildingType = request.getString("buildingType");
        } catch (RuntimeException exception) {
            return HttpResponses.error(
                "INVALID_PARAMETER",
                "provinceId must be an integer and buildingType must be a string."
            );
        }

        if (provinceId == null || buildingType == null) {
            return HttpResponses.error(
                "MISSING_PARAMETER",
                "provinceId and buildingType are required."
            );
        }

        if (
            CFG.gameAction.getActiveTurnStateID() !=
            GameAction.TurnStates.INPUT_ORDERS
        ) {
            return HttpResponses.error(
                "NOT_ACCEPTING_ORDERS",
                "The game is not accepting orders now."
            );
        }

        if (provinceId < 0 || provinceId >= CFG.core.getProvinSize()) {
            return HttpResponses.error(
                "INVALID_PROVINCE_ID",
                "provinceId is out of range."
            );
        }

        int civilizationId = CFG.core.getPlayer(CFG.PLAYER_TURN_ID).getCivId();
        Civilization civilization = CFG.core.getCiv(civilizationId);
        Province province = CFG.core.getProv(provinceId);

        if (province.getCivId() != civilizationId) {
            return HttpResponses.error(
                "PROVINCE_NOT_OWNED",
                "The province is not owned by the player."
            );
        }

        if (province.isOccupied()) {
            return HttpResponses.error(
                "PROVINCE_OCCUPIED",
                "Buildings cannot be constructed in an occupied province."
            );
        }

        String type = buildingType.trim().toLowerCase(Locale.ROOT);
        int currentLevel;
        int targetLevel;
        int maximumLevel;
        int goldCost;
        int movementCost;
        int constructionTurns;
        float requiredTechnology;
        ConstructionType constructionType;
        boolean accepted;

        switch (type) {
            case "farm":
                currentLevel = province.getLvlOfFarm();
                targetLevel = currentLevel + 1;
                maximumLevel = BuildingsManager.getFarm_MaxLevel();
                goldCost = BuildingsManager.getFarm_BuildCost(
                    targetLevel,
                    provinceId
                );
                movementCost = BuildingsManager.getFarm_BuildMovementCost(
                    targetLevel
                );
                constructionTurns = BuildingsManager.getFarm_Construction(
                    targetLevel
                );
                requiredTechnology = BuildingsManager.getFarm_TechLevel(
                    targetLevel
                );
                constructionType = ConstructionType.FARM;

                if (!BuildingsManager.canBuildFarm_Terrain(provinceId)) {
                    return HttpResponses.error(
                        "INVALID_TERRAIN",
                        "A farm cannot be constructed on this terrain."
                    );
                }
                break;
            case "port":
                currentLevel = province.getLvlOfPort();
                targetLevel = currentLevel + 1;
                maximumLevel = BuildingsManager.getPort_MaxLevel();
                goldCost = BuildingsManager.getPort_BuildCost(
                    targetLevel,
                    provinceId
                );
                movementCost = BuildingsManager.getPort_BuildMovementCost(
                    targetLevel
                );
                constructionTurns = BuildingsManager.getPort_Construction(
                    targetLevel
                );
                requiredTechnology = BuildingsManager.getPort_TechLevel(
                    targetLevel
                );
                constructionType = ConstructionType.PORT;

                if (!(province.getNeighSeaProvincesSize() > 0)) {
                    return HttpResponses.error(
                        "INVALID_TERRAIN",
                        "A port cannot be constructed on inland provinces."
                    );
                }
                break;
            case "fort":
                currentLevel = province.getLvlOfFort();
                targetLevel = currentLevel + 1;
                maximumLevel = BuildingsManager.getFort_MaxLevel();
                goldCost = BuildingsManager.getFort_BuildCost(
                    targetLevel,
                    provinceId
                );
                movementCost = BuildingsManager.getFort_BuildMovementCost(
                    targetLevel
                );
                constructionTurns = BuildingsManager.getFort_Construction(
                    targetLevel
                );
                requiredTechnology = BuildingsManager.getFort_TechLevel(
                    targetLevel
                );
                constructionType = ConstructionType.FORT;

                break;
            case "tower":
                currentLevel = province.getLvlOfWatchTower();
                targetLevel = currentLevel + 1;
                maximumLevel = BuildingsManager.getTower_MaxLevel();
                goldCost = BuildingsManager.getTower_BuildCost(
                    targetLevel,
                    provinceId
                );
                movementCost = BuildingsManager.getTower_BuildMovementCost(
                    targetLevel
                );
                constructionTurns = BuildingsManager.getTower_Construction(
                    targetLevel
                );
                requiredTechnology = BuildingsManager.getTower_TechLevel(
                    targetLevel
                );
                constructionType = ConstructionType.TOWER;

                break;
            case "library":
                currentLevel = province.getLvlOfLibrary();
                targetLevel = currentLevel + 1;
                maximumLevel = BuildingsManager.getLibrary_MaxLevel();
                goldCost = BuildingsManager.getLibrary_BuildCost(
                    targetLevel,
                    provinceId
                );
                movementCost = BuildingsManager.getLibrary_BuildMovementCost(
                    targetLevel
                );
                constructionTurns = BuildingsManager.getLibrary_Construction(
                    targetLevel
                );
                requiredTechnology = BuildingsManager.getLibrary_TechLevel(
                    targetLevel
                );
                constructionType = ConstructionType.LIBRARY;

                break;
            case "armoury":
                currentLevel = province.getLvlOfArmoury();
                targetLevel = currentLevel + 1;
                maximumLevel = BuildingsManager.getArmoury_MaxLevel();
                goldCost = BuildingsManager.getArmoury_BuildCost(
                    targetLevel,
                    provinceId
                );
                movementCost = BuildingsManager.getArmoury_BuildMovementCost(
                    targetLevel
                );
                constructionTurns = BuildingsManager.getArmoury_Construction(
                    targetLevel
                );
                requiredTechnology = BuildingsManager.getArmoury_TechLevel(
                    targetLevel
                );
                constructionType = ConstructionType.ARMOURY;

                break;
            case "workshop":
                currentLevel = province.getLvlOfWorkshop();
                targetLevel = currentLevel + 1;
                maximumLevel = BuildingsManager.getWorkshop_MaxLevel();
                goldCost = BuildingsManager.getWorkshop_BuildCost(
                    targetLevel,
                    provinceId
                );
                movementCost = BuildingsManager.getWorkshop_BuildMovementCost(
                    targetLevel
                );
                constructionTurns = BuildingsManager.getWorkshop_Construction(
                    targetLevel
                );
                requiredTechnology = BuildingsManager.getWorkshop_TechLevel(
                    targetLevel
                );
                constructionType = ConstructionType.WORKSHOP;

                break;
            case "market":
                currentLevel = province.getLvlOfMarket();
                targetLevel = currentLevel + 1;
                maximumLevel = BuildingsManager.getMarket_MaxLevel();
                goldCost = BuildingsManager.getMarket_BuildCost(
                    targetLevel,
                    provinceId
                );
                movementCost = BuildingsManager.getMarket_BuildMovementCost(
                    targetLevel
                );
                constructionTurns = BuildingsManager.getMarket_Construction(
                    targetLevel
                );
                requiredTechnology = BuildingsManager.getMarket_TechLevel(
                    targetLevel
                );
                constructionType = ConstructionType.MARKET;

                break;
            case "supply":
                    currentLevel = province.getLvlOfSupply();
                    targetLevel = currentLevel + 1;
                    maximumLevel = BuildingsManager.getSupply_MaxLevel();
                    goldCost = BuildingsManager.getSupply_BuildCost(
                        targetLevel,
                        provinceId
                    );
                    movementCost = BuildingsManager.getSupply_BuildMovementCost(
                        targetLevel
                    );
                    constructionTurns = BuildingsManager.getSupply_Construction(
                        targetLevel
                    );
                    requiredTechnology = BuildingsManager.getSupply_TechLevel(
                        targetLevel
                    );
                    constructionType = ConstructionType.SUPPLY;

                    break;
            default:
                return HttpResponses.error(
                    "INVALID_BUILDING_TYPE",
                    "Unsupported buildingType."
                );
        }

        if (province.getSeaProv()) {
            return HttpResponses.error(
                "SEA_PROVINCE",
                "Buildings cannot be constructed in a sea province."
            );
        }

        if (currentLevel >= maximumLevel) {
            return HttpResponses.error(
                "MAX_LEVEL",
                "The building is already at its maximum level."
            );
        }

        if (civilization.isInConstruction(provinceId, constructionType) > 0) {
            return HttpResponses.error(
                "ALREADY_IN_CONSTRUCTION",
                "This building is already under construction."
            );
        }

        if (civilization.getTechLevel() < requiredTechnology) {
            return HttpResponses.error(
                "TECH_LEVEL_TOO_LOW",
                "The civilization does not meet the technology requirement."
            );
        }

        if (civilization.getMovemPoints() < movementCost) {
            return HttpResponses.error(
                "NOT_ENOUGH_MOVEMENT_POINTS",
                "The civilization does not have enough movement points."
            );
        }

        if (civilization.getGold() < goldCost) {
            return HttpResponses.error(
                "NOT_ENOUGH_GOLD",
                "The civilization does not have enough gold."
            );
        }

        switch (type) {
            case "farm":
                accepted = BuildingsManager.constructFarm(
                    provinceId,
                    civilizationId
                );
                break;
            case "fort":
                accepted = BuildingsManager.constructFort(
                    provinceId,
                    civilizationId
                );
                break;
            case "tower":
                accepted = BuildingsManager.constructTower(
                    provinceId,
                    civilizationId
                );
                break;
            case "port":
                accepted = BuildingsManager.constructPort(
                    provinceId,
                    civilizationId
                );
                break;
            case "library":
                accepted = BuildingsManager.constructLibrary(
                    provinceId,
                    civilizationId
                );
                break;
            case "armoury":
                accepted = BuildingsManager.constructArmoury(
                    provinceId,
                    civilizationId
                );
                break;
            case "workshop":
                accepted = BuildingsManager.constructWorkshop(
                    provinceId,
                    civilizationId
                );
                break;
            case "market":
                accepted = BuildingsManager.constructMarket(
                    provinceId,
                    civilizationId
                );
                break;
            case "supply":
                accepted = BuildingsManager.constructSupply(
                    provinceId,
                    civilizationId
                );
                break;
            default:
                accepted = false;
        }

        if (!accepted) {
            return HttpResponses.error(
                "CONSTRUCTION_REJECTED",
                "The game rejected the construction order."
            );
        }

        CFG.core.getPlayer(CFG.PLAYER_TURN_ID).setNoOrders(false);
        CFG.menus.updateInGameTopAll(civilizationId);

        JSONObject result = new JSONObject();
        result.put("provinceId", provinceId);
        result.put("buildingType", type);
        result.put("civilizationId", civilizationId);
        result.put("currentLevel", currentLevel);
        result.put("targetLevel", targetLevel);
        result.put("constructionTurns", constructionTurns);
        result.put("goldCost", goldCost);
        result.put("movementCost", movementCost);
        result.put("remainingGold", civilization.getGold());
        result.put("remainingMovementPoints", civilization.getMovemPoints());
        return HttpResponses.success(result);
    }
}
