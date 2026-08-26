package top.tasaed.aoh2de.llm.playing.handlers;

import age.of.civilizations2.jakowski.lukasz.CFG;
import age.of.civilizations2.jakowski.lukasz.Civilization;
import com.alibaba.fastjson2.JSONObject;
import top.tasaed.aoh2de.llm.playing.HttpResponses;

public final class BudgetSpendingInfoHandler extends GameRequestHandler {
    public BudgetSpendingInfoHandler() {
        super("GET_BUDGET_SPENDING_INFO_FAILED", "Failed to get budget spending info.");
    }

    @Override
    protected JSONObject handleOnGameThread(JSONObject request) {
        int civilizationId = CFG.core.getPlayer(CFG.PLAYER_TURN_ID).getCivId();
        Civilization civilization = CFG.core.getCiv(civilizationId);
        int ideologyId = civilization.getIdeology();

        // Get current spending values
        float currentGoods = civilization.getSpendingGoodsB() * 100.0f;
        float currentResearch = civilization.getSpendingResearchB() * 100.0f;
        float currentInvestments = civilization.getSpendingInvestmentsB() * 100.0f;
        float currentTaxes = civilization.getTaxationLvl() * 100.0f;
        int currentMilitary = CFG.gameUpdate.getMilitarySpending(civilizationId, civilization.iBudget);

        // Get threshold values (negative growth thresholds)
        float minGoods = CFG.ideologiesMgr.getIdeologyID(ideologyId).getMin_Goods(civilizationId) * 100.0f;
        float minInvestments = CFG.ideologiesMgr.getInvestments(ideologyId, civilizationId) * 100.0f;
        float maxTaxes = CFG.ideologiesMgr.getAcceptableTaxation(ideologyId, civilizationId) * 100.0f;

        JSONObject result = new JSONObject();
        result.put("budget", civilization.iBudget);

        // Current spending
        JSONObject current = new JSONObject();
        current.put("goods", currentGoods);
        current.put("research", currentResearch);
        current.put("investments", currentInvestments);
        current.put("taxes", currentTaxes);
        current.put("military", currentMilitary);
        result.put("current", current);

        // Thresholds (negative growth values)
        JSONObject thresholds = new JSONObject();
        thresholds.put("min_goods", minGoods); // Below this value: population negative growth (人口负增长)
        thresholds.put("min_investments", minInvestments); // Below this value: economy negative growth (经济负增长)
        thresholds.put("max_taxes", maxTaxes); // Above this value: happiness negative growth (幸福度负增长)
        result.put("thresholds", thresholds);

        // Status indicators
        JSONObject status = new JSONObject();
        status.put("goods_sufficient", currentGoods >= minGoods);
        status.put("investments_sufficient", currentInvestments >= minInvestments);
        status.put("taxes_acceptable", currentTaxes <= maxTaxes);
        result.put("status", status);

        return HttpResponses.success(result);
    }
}
