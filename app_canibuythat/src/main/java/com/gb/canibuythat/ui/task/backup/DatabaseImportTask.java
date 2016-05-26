package com.gb.canibuythat.ui.task.backup;

import android.os.AsyncTask;

import com.gb.canibuythat.App;
import com.gb.canibuythat.provider.BudgetDbHelper;
import com.gb.canibuythat.provider.BudgetProvider;
import com.gb.canibuythat.provider.Contract;
import com.gb.canibuythat.util.DBUtils;

import java.io.File;

public class DatabaseImportTask extends AsyncTask<Void, Void, Void> {

    private String path;

    public DatabaseImportTask(String path) {
        this.path = path;
    }

    @Override protected Void doInBackground(Void... params) {
        BudgetDbHelper helper = BudgetDbHelper.get();
        DBUtils.importDatabase(new File(path), Contract.BudgetItem.TABLE,
                Contract.BudgetItem.COLUMNS, helper);
        App.getAppContext()
                .getContentResolver()
                .notifyChange(BudgetProvider.BUDGET_ITEMS_URI, null);
        return null;
    }
}