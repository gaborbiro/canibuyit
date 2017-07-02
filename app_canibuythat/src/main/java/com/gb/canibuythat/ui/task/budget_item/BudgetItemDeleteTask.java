package com.gb.canibuythat.ui.task.budget_item;

import com.gb.canibuythat.App;
import com.gb.canibuythat.model.BudgetItem;
import com.gb.canibuythat.provider.BudgetProvider;
import com.gb.canibuythat.ui.task.Callback;
import com.j256.ormlite.dao.Dao;

import java.sql.SQLException;

public class BudgetItemDeleteTask extends SQLTaskBase<Boolean> {

    private int id;

    public BudgetItemDeleteTask(Callback<Boolean> callback, int id) {
        super(callback);
        this.id = id;
    }

    @Override
    protected Boolean doWork(Dao<BudgetItem, Integer> dao) throws SQLException {
        boolean success = dao.deleteById(id) > 0;
        App.getAppContext().getContentResolver().notifyChange(BudgetProvider.BUDGET_ITEMS_URI, null);
        return success;
    }
}