package top.tasaed.aoh2de.llm.playing.handlers;

import age.of.civilizations2.jakowski.lukasz.CFG;
import age.of.civilizations2.jakowski.lukasz.Civilization;
import age.of.civilizations2.jakowski.lukasz.GameAction;
import age.of.civilizations2.jakowski.lukasz.GameValues.GameValues;
import com.alibaba.fastjson2.JSONObject;
import top.tasaed.aoh2de.llm.playing.HttpResponses;

public final class BudgetSpendingHandler extends GameRequestHandler {
    public BudgetSpendingHandler() {
        super("SET_BUDGET_SPENDING_FAILED", "Failed to set budget spending.");
    }

    @Override
    protected JSONObject handleOnGameThread(JSONObject request) {
        Float goods;
        Float research;
        Float investments;
        Float taxes;
        try {
            goods = request.getFloat("goods");
            research = request.getFloat("research");
            investments = request.getFloat("investments");
            taxes = request.getFloat("taxes");
        } catch (RuntimeException exception) {
            return HttpResponses.error(
                    "INVALID_PARAMETER", "goods, research, investments and taxes must be numbers from 0 to 100.");
        }

        if (goods == null || research == null || investments == null || taxes == null) {
            return HttpResponses.error("MISSING_PARAMETER", "goods, research, investments and taxes are required.");
        }
        if (!isPercentage(goods) || !isPercentage(research) || !isPercentage(investments) || !isPercentage(taxes)) {
            return HttpResponses.error(
                    "INVALID_PARAMETER", "goods, research, investments and taxes must be numbers from 0 to 100.");
        }
        if (CFG.gameAction.getActiveTurnStateID() != GameAction.TurnStates.INPUT_ORDERS) {
            return HttpResponses.error("NOT_ACCEPTING_ORDERS", "The game is not accepting orders now.");
        }

        int civilizationId = CFG.core.getPlayer(CFG.PLAYER_TURN_ID).getCivId();
        Civilization civilization = CFG.core.getCiv(civilizationId);
        if (civilization.iBudget <= 0) {
            goods = 0.0f;
            research = 0.0f;
            investments = 0.0f;
        } else {
            int military = CFG.gameUpdate.getMilitarySpending(civilizationId, civilization.iBudget);
            if (goods + research + investments + military > GameValues.gvAiBudget.BUDGET_MAX) {
                return HttpResponses.error("BUDGET_EXCEEDED", "The requested spending exceeds the available budget.");
            }
        }

        civilization.setSpendingGoodsB(goods / 100.0f);
        civilization.setSpendingResearchB(research / 100.0f);
        civilization.setSpendingInvestmentsB(investments / 100.0f);
        civilization.setTaxationLvl(taxes / 100.0f);
        CFG.gameUpdate.getBalance_UpdateBudgetPrepare(civilizationId);
        CFG.gameUpdate.updateSpendingOfCivID(civilizationId, civilization.iBudget);
        CFG.menus.updateInGameTopAll(civilizationId);

        JSONObject result = new JSONObject();
        result.put("goods", civilization.getSpendingGoodsB() * 100.0f);
        result.put("research", civilization.getSpendingResearchB() * 100.0f);
        result.put("investments", civilization.getSpendingInvestmentsB() * 100.0f);
        result.put("taxes", civilization.getTaxationLvl() * 100.0f);
        result.put("military", CFG.gameUpdate.getMilitarySpending(civilizationId, civilization.iBudget));
        result.put("budget", civilization.iBudget);
        return HttpResponses.success(result);
    }

    private static boolean isPercentage(float value) {
        return !Float.isNaN(value) && !Float.isInfinite(value) && value >= 0.0f && value <= 100.0f;
    }
}
