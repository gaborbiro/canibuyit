package com.gb.canibuythat.provider;


import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.gb.canibuythat.App;
import com.gb.canibuythat.model.BudgetModifier;
import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;


/**
 * Created by gbiro on 1/7/2015.
 */
public class BudgetModifierDbHelper extends OrmLiteSqliteOpenHelper {

    public static final String DATABASE_NAME = "budget.sqlite";
    private static final int DATABASE_VERSION = 1;

    private static BudgetModifierDbHelper INSTANCE;


    public BudgetModifierDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static BudgetModifierDbHelper get() {
        if (INSTANCE == null) {
            INSTANCE = OpenHelperManager.getHelper(App.getAppContext(), BudgetModifierDbHelper.class);
        }
        return INSTANCE;
    }

    @Override
    public void onCreate(SQLiteDatabase database, ConnectionSource connectionSource) {
        try {
            TableUtils.createTable(connectionSource, BudgetModifier.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, ConnectionSource connectionSource, int oldVersion, int newVersion) {

    }


    public void cleanup() {
        if (INSTANCE != null) {
            OpenHelperManager.releaseHelper();
            INSTANCE = null;
        }
    }
}
