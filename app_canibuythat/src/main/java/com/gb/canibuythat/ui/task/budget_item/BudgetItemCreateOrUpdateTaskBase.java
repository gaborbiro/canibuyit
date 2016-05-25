package com.gb.canibuythat.ui.task.budget_item;

import com.gb.canibuythat.App;
import com.gb.canibuythat.model.BudgetItem;
import com.gb.canibuythat.provider.BudgetProvider;
import com.gb.canibuythat.ui.BudgetItemUpdatedEvent;
import com.gb.canibuythat.ui.task.Callback;
import com.j256.ormlite.dao.Dao;

import org.greenrobot.eventbus.EventBus;

import java.sql.SQLException;

public class BudgetItemCreateOrUpdateTaskBase
        extends SQLTaskBase<Dao.CreateOrUpdateStatus> {

    private BudgetItem mBudgetItem;

    public BudgetItemCreateOrUpdateTaskBase(BudgetItem budgetItem,
            Callback<Dao.CreateOrUpdateStatus> callback) {
        super(callback);
        this.mBudgetItem = budgetItem;
    }

    @Override protected Dao.CreateOrUpdateStatus doWork(Dao<BudgetItem, Integer> dao)
            throws SQLException {
        Dao.CreateOrUpdateStatus result = dao.createOrUpdate(mBudgetItem);
        App.getAppContext()
                .getContentResolver()
                .notifyChange(BudgetProvider.BUDGET_ITEMS_URI, null);
        EventBus.getDefault()
                .post(new BudgetItemUpdatedEvent());
        return result;
    }
}