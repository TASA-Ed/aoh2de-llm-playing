package top.tasaed.aoh2de.llm.playing.handlers;

import age.of.civilizations2.jakowski.lukasz.CFG;
import age.of.civilizations2.jakowski.lukasz.Province;
import com.alibaba.fastjson2.JSONObject;
import top.tasaed.aoh2de.llm.playing.HttpResponses;

public final class ProvinceInformationHandler extends GameRequestHandler {
    public ProvinceInformationHandler() {
        super("GET_PROVINCE_INFORMATION_FAILED", "Failed to get the province information.");
    }

    @Override
    protected JSONObject handleOnGameThread(JSONObject request) {
        Integer provinceId;

        try {
            provinceId = request.getInteger("provinceId");
        } catch (RuntimeException exception) {
            return HttpResponses.error("INVALID_PARAMETER", "provinceId must be an integer.");
        }

        if (provinceId == null) {
            return HttpResponses.error("MISSING_PARAMETER", "provinceId is required.");
        }

        if (provinceId < 0 || provinceId >= CFG.core.getProvinSize()) {
            return HttpResponses.error("INVALID_PROVINCE_ID", "Province ID is out of range.");
        }

        Province province = CFG.core.getProv(provinceId);
        JSONObject information = new JSONObject();

        information.put("id", province.getProvID());
        information.put("name", province.getName());
        information.put("terrainTypeId", province.getTerrainTypeID());
        information.put("isCoastal", province.getNeighSeaProvincesSize() > 0);

        information.put("population", province.getPop().getPops());
        information.put("economy", province.getEco());
        information.put("developmentLevel", province.getDeveLvl());

        information.put("civilizationId", province.getCivId());

        JSONObject buildings = new JSONObject();

        int portLevel = province.getLvlOfPort();
        if (portLevel > 0) buildings.put("port", portLevel);

        int farmLevel = province.getLvlOfFarm();
        if (farmLevel > 0) buildings.put("farm", farmLevel);

        int workshopLevel = province.getLvlOfWorkshop();
        if (workshopLevel > 0) buildings.put("workshop", workshopLevel);

        int marketLevel = province.getLvlOfMarket();
        if (marketLevel > 0) buildings.put("market", marketLevel);

        int libraryLevel = province.getLvlOfLibrary();
        if (libraryLevel > 0) buildings.put("library", libraryLevel);

        int armouryLevel = province.getLvlOfArmoury();
        if (armouryLevel > 0) buildings.put("armoury", armouryLevel);

        int supplyLevel = province.getLvlOfSupply();
        if (supplyLevel > 0) buildings.put("supply", supplyLevel);

        int fortLevel = province.getLvlOfFort();
        if (fortLevel > 0) buildings.put("fort", fortLevel);

        int watchTowerLevel = province.getLvlOfWatchTower();
        if (watchTowerLevel > 0) buildings.put("watchTower", watchTowerLevel);

        information.put("buildings", buildings);

        return HttpResponses.success(information);
    }
}
