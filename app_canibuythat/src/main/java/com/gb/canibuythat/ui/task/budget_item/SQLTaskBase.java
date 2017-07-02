package com.gb.canibuythat.ui.task.budget_item;

import com.gb.canibuythat.model.BudgetItem;
import com.gb.canibuythat.provider.BudgetDbHelper;
import com.gb.canibuythat.ui.task.Callback;
import com.gb.canibuythat.ui.task.TaskBase;
import com.j256.ormlite.dao.Dao;

import java.sql.SQLException;

public abstract class SQLTaskBase<R> extends TaskBase<R> {

    public SQLTaskBase(Callback<R> callback) {
        super(callback);
    }

    @Override
    protected R doWork() throws SQLException {
        Dao<BudgetItem, Integer> dao = BudgetDbHelper.get().getDao(BudgetItem.class);
        return doWork(dao);
    }

    protected abstract R doWork(Dao<BudgetItem, Integer> dao) throws SQLException;
}
