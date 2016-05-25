package com.gb.canibuythat.ui.task.balance_update;

import android.os.AsyncTask;

import com.gb.canibuythat.model.BalanceUpdateEvent;
import com.gb.canibuythat.provider.BudgetDbHelper;
import com.j256.ormlite.dao.Dao;

import java.sql.SQLException;

public class BalanceUpdateWriterTask extends AsyncTask<BalanceUpdateEvent, Void, Void> {

    @Override protected Void doInBackground(BalanceUpdateEvent... params) {
        BudgetDbHelper helper = BudgetDbHelper.get();

        try {
            Dao<BalanceUpdateEvent, Integer> dao =
                    helper.getDao(BalanceUpdateEvent.class);

            for (BalanceUpdateEvent balanceUpdateEvent : params) {
                dao.create(balanceUpdateEvent);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}