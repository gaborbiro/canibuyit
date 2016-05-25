package com.gb.canibuythat.ui.task.balance_update;

import com.gb.canibuythat.model.BalanceUpdateEvent;
import com.gb.canibuythat.model.BudgetItem;
import com.gb.canibuythat.provider.BalanceCalculator;
import com.gb.canibuythat.provider.BudgetDbHelper;
import com.gb.canibuythat.ui.task.Callback;
import com.gb.canibuythat.ui.task.TaskBase;
import com.j256.ormlite.dao.Dao;

import java.util.Date;

public class CalculateBalanceTask extends TaskBase<CalculateBalanceTask.BalanceResult> {

    public static class BalanceResult {
        public final BalanceUpdateEvent lastBalanceUpdateEvent;
        public final float bestCaseBalance;
        public final float worstCaseBalance;

        public BalanceResult(BalanceUpdateEvent lastBalanceUpdateEvent,
                float bestCaseBalance, float worstCaseBalance) {
            this.lastBalanceUpdateEvent = lastBalanceUpdateEvent;
            this.bestCaseBalance = bestCaseBalance;
            this.worstCaseBalance = worstCaseBalance;
        }
    }


    private LastBalanceUpdateLoaderTask mLastBalanceUpdateLoaderTask;

    public CalculateBalanceTask(Callback<BalanceResult> callback) {
        super(callback);
    }

    @Override protected void onPreExecute() {
        mLastBalanceUpdateLoaderTask = new LastBalanceUpdateLoaderTask();
        mLastBalanceUpdateLoaderTask.execute();
    }

    @Override protected BalanceResult doWork() throws Exception {
        BudgetDbHelper helper = BudgetDbHelper.get();
        float bestCase = 0;
        float worstCase = 0;

        // blocking thread
        BalanceUpdateEvent balanceUpdateEvent = mLastBalanceUpdateLoaderTask.get();
        Dao<BudgetItem, Integer> budgetItemDao =
                helper.getDao(com.gb.canibuythat.model.BudgetItem.class);

        for (com.gb.canibuythat.model.BudgetItem item : budgetItemDao) {
            if (item.mEnabled) {
                BalanceCalculator.BalanceResult result = BalanceCalculator.get()
                        .getEstimatedBalance(item,
                                balanceUpdateEvent != null ? balanceUpdateEvent.when
                                                           : null, new Date());
                bestCase += result.bestCase;
                worstCase += result.worstCase;
            }
        }

        if (balanceUpdateEvent != null) {
            bestCase += balanceUpdateEvent.value;
            worstCase += balanceUpdateEvent.value;
        }
        return new BalanceResult(balanceUpdateEvent, bestCase, worstCase);
    }
}