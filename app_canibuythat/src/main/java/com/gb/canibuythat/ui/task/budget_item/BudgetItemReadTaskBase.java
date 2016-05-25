package com.gb.canibuythat.ui.task.budget_item;

import com.gb.canibuythat.model.BudgetItem;
import com.gb.canibuythat.ui.task.Callback;
import com.j256.ormlite.dao.Dao;

import java.sql.SQLException;

public class BudgetItemReadTaskBase extends SQLTaskBase<BudgetItem> {

    private int mBudgetItemId;

    public BudgetItemReadTaskBase(int budgetItemId, Callback<BudgetItem> callback) {
        super(callback);
        this.mBudgetItemId = budgetItemId;
    }

    @Override protected BudgetItem doWork(Dao<BudgetItem, Integer> dao)
            throws SQLException {
        return dao.queryForId(mBudgetItemId);
    }
}