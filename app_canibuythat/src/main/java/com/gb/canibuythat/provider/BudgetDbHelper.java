package com.gb.canibuythat.provider;


import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;

import com.gb.canibuythat.App;
import com.gb.canibuythat.model.BudgetItem;
import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class BudgetDbHelper extends OrmLiteSqliteOpenHelper {

    public static final String DATABASE_NAME = "budget.sqlite";
    private static final int DATABASE_VERSION = 2;

    private static BudgetDbHelper INSTANCE;

    public BudgetDbHelper(Context context) {

        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static BudgetDbHelper get() {
        if (INSTANCE == null) {
            INSTANCE = OpenHelperManager.getHelper(App.getAppContext(), BudgetDbHelper.class);
        }
        return INSTANCE;
    }

    @Override
    public void onCreate(SQLiteDatabase database, ConnectionSource connectionSource) {
        try {
            TableUtils.createTable(connectionSource, BudgetItem.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, ConnectionSource connectionSource,
                          int oldVersion, int newVersion) {
        if (oldVersion == 1 && newVersion == 2) {
            database.beginTransaction();
            database.execSQL("ALTER TABLE " + Contract.BudgetItem.TABLE +
                    " ADD COLUMN " + Contract.BudgetItem.ORDERING + " INTEGER DEFAULT 0");
            database.setTransactionSuccessful();
            database.endTransaction();
            SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
            queryBuilder.setTables(Contract.BudgetItem.TABLE);
            Cursor cursor = queryBuilder.query(database, new String[]{Contract.BudgetItem._ID}, null, null, null, null, null);
            int length = cursor.getCount();
            Map<Integer, Integer> indexMap = new HashMap<>();
            for (int i = 0; i < length; i++) {
                cursor.moveToNext();
                indexMap.put(i, cursor.getInt(0));
            }

            cursor.close();

            database.beginTransaction();
            for (int i = 0; i < length; i++) {
                database.execSQL("UPDATE " + Contract.BudgetItem.TABLE +
                        " SET " + Contract.BudgetItem.ORDERING + " = " + i + " WHERE " +
                        Contract.BudgetItem._ID + " = " + indexMap.get(i));
            }
            database.setTransactionSuccessful();
            database.endTransaction();
        }
    }


    public void cleanup() {
        if (INSTANCE != null) {
            OpenHelperManager.releaseHelper();
            INSTANCE = null;
        }
    }
}
