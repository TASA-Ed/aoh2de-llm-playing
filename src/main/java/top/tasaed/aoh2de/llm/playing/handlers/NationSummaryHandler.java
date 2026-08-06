package top.tasaed.aoh2de.llm.playing.handlers;

import top.tasaed.aoh2de.llm.playing.HttpResponses;

import age.of.civilizations2.jakowski.lukasz.CFG;
import age.of.civilizations2.jakowski.lukasz.Civilization;
import com.alibaba.fastjson2.JSONObject;

public final class NationSummaryHandler extends GameRequestHandler {
    public NationSummaryHandler() {
        super("GET_NATION_SUMMARY_FAILED", "Failed to get the nation summary.");
    }

    @Override
    protected JSONObject handleOnGameThread() {
        Civilization player = CFG.core.getCiv(CFG.core.getPlayer(CFG.PLAYER_TURN_ID).getCivId());
        JSONObject summary = new JSONObject();
        summary.put("gold", player.getGold());
        summary.put("movePoints", player.getMovemPoints());
        summary.put("diplomacyPoints", player.getDiploPoints());
        summary.put("technologyLevel", player.getTechLevel());
        summary.put("stability", player.getStabilityCiv());
        summary.put("happiness", player.getHappiness());
        summary.put("population", player.countPop());
        summary.put("economy", player.countEco());
        summary.put("provinceCount", player.getNumOfProvs());
        summary.put("unitCount", player.getNumberOfUnits());
        summary.put("income", CFG.gameUpdate.getIncome(player.getCivId()));
        summary.put("expenses", CFG.gameUpdate.getExpenses(player.getCivId()));
        summary.put("warWeariness", player.getWarWeariness());
        summary.put("atWar", player.isAtWarC());
        return HttpResponses.success(summary);
    }
}
