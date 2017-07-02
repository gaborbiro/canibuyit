package com.gb.canibuythat.ui.task.budget_item;

import com.gb.canibuythat.model.BudgetItem;
import com.gb.canibuythat.ui.task.Callback;
import com.j256.ormlite.dao.Dao;

import java.sql.SQLException;

public class BudgetItemReadTask extends SQLTaskBase<BudgetItem> {

    private int budgetItemId;

    public BudgetItemReadTask(int budgetItemId, Callback<BudgetItem> callback) {
        super(callback);
        this.budgetItemId = budgetItemId;
    }

    @Override
    protected BudgetItem doWork(Dao<BudgetItem, Integer> dao) throws SQLException {
        return dao.queryForId(budgetItemId);
    }
}