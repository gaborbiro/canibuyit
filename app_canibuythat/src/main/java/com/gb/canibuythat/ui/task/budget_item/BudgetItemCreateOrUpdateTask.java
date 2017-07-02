package com.gb.canibuythat.ui.task.budget_item;

import com.gb.canibuythat.App;
import com.gb.canibuythat.model.BudgetItem;
import com.gb.canibuythat.provider.BudgetProvider;
import com.gb.canibuythat.ui.task.Callback;
import com.j256.ormlite.dao.Dao;

import java.sql.SQLException;

public class BudgetItemCreateOrUpdateTask extends SQLTaskBase<Dao.CreateOrUpdateStatus> {

    private BudgetItem budgetItem;

    public BudgetItemCreateOrUpdateTask(BudgetItem budgetItem, Callback<Dao.CreateOrUpdateStatus> callback) {
        super(callback);
        this.budgetItem = budgetItem;
    }

    @Override
    protected Dao.CreateOrUpdateStatus doWork(Dao<BudgetItem, Integer> dao) throws SQLException {
        Dao.CreateOrUpdateStatus result = dao.createOrUpdate(budgetItem);
        App.getAppContext().getContentResolver().notifyChange(BudgetProvider.BUDGET_ITEMS_URI, null);
        return result;
    }
}