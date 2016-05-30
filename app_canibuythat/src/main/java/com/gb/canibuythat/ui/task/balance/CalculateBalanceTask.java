package com.gb.canibuythat.ui.task.balance;

import com.gb.canibuythat.UserPreferences;
import com.gb.canibuythat.model.BudgetItem;
import com.gb.canibuythat.provider.BalanceCalculator;
import com.gb.canibuythat.provider.BudgetDbHelper;
import com.gb.canibuythat.ui.model.BalanceReading;
import com.gb.canibuythat.ui.task.Callback;
import com.gb.canibuythat.ui.task.TaskBase;
import com.j256.ormlite.dao.Dao;

import java.util.Date;

public class CalculateBalanceTask extends TaskBase<CalculateBalanceTask.BalanceResult> {

    public static class BalanceResult {
        public final BalanceReading balanceReading;
        public final float bestCaseBalance;
        public final float worstCaseBalance;

        public BalanceResult(BalanceReading balanceReading,
                float bestCaseBalance, float worstCaseBalance) {
            this.balanceReading = balanceReading;
            this.bestCaseBalance = bestCaseBalance;
            this.worstCaseBalance = worstCaseBalance;
        }
    }


    public CalculateBalanceTask(Callback<BalanceResult> callback) {
        super(callback);
    }

    @Override protected BalanceResult doWork() throws Exception {
        BudgetDbHelper helper = BudgetDbHelper.get();
        float bestCase = 0;
        float worstCase = 0;

        // blocking thread
        BalanceReading balanceReading = UserPreferences.getBalanceReading();
        Dao<BudgetItem, Integer> budgetItemDao =
                helper.getDao(com.gb.canibuythat.model.BudgetItem.class);

        for (com.gb.canibuythat.model.BudgetItem item : budgetItemDao) {
            if (item.mEnabled) {
                Date startDate =
                        balanceReading != null ? balanceReading.when : null;
                BalanceCalculator.BalanceResult result = BalanceCalculator.get()
                        .getEstimatedBalance(item, startDate,
                                UserPreferences.getEstimateDate());
                bestCase += result.bestCase;
                worstCase += result.worstCase;
            }
        }

        if (balanceReading != null) {
            bestCase += balanceReading.balance;
            worstCase += balanceReading.balance;
        }
        return new BalanceResult(balanceReading, bestCase, worstCase);
    }
}