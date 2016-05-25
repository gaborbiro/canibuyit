package com.gb.canibuythat.ui.task.balance_update;

import android.os.AsyncTask;

import com.gb.canibuythat.model.BalanceUpdateEvent;
import com.gb.canibuythat.provider.BudgetDbHelper;
import com.gb.canibuythat.provider.Contract;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;

import java.sql.SQLException;
import java.util.List;

public class LastBalanceUpdateLoaderTask
        extends AsyncTask<Void, Void, BalanceUpdateEvent> {

    @Override
    protected BalanceUpdateEvent doInBackground(Void... params) {
        BudgetDbHelper helper = BudgetDbHelper.get();
        try {
            Dao<BalanceUpdateEvent, Integer> dao =
                    helper.getDao(BalanceUpdateEvent.class);
            QueryBuilder<BalanceUpdateEvent, Integer> qBuilder = dao.queryBuilder();
            qBuilder.orderBy(Contract.BalanceUpdateEvent._ID, false);
            qBuilder.limit(1L);
            List<BalanceUpdateEvent> listOfOne = qBuilder.query();

            if (listOfOne != null && !listOfOne.isEmpty()) {
                return listOfOne.get(0);
            } else {
                return null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}