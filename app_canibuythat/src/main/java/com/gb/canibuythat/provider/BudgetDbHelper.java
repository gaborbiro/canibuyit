package com.gb.canibuythat.provider;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import com.gb.canibuythat.model.BudgetItem;
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;

import javax.inject.Inject;

public class BudgetDbHelper extends OrmLiteSqliteOpenHelper {

    public static final String DATABASE_NAME = "budget.sqlite";
    private static final int DATABASE_VERSION = 1;

    @Inject
    public BudgetDbHelper(Context appContext) {
        super(appContext, DATABASE_NAME, null, DATABASE_VERSION);
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
    public void onUpgrade(SQLiteDatabase database, ConnectionSource connectionSource, int oldVersion, int newVersion) {
    }

    public SQLiteDatabase getDatabaseFromFile(String file) throws SQLiteException {
        return SQLiteDatabase.openDatabase(file, null, 0);
    }

    public Cursor getAllBudgetItems(SQLiteDatabase db) throws SQLException {
        return db.query(Contract.BudgetItem.TABLE, Contract.BudgetItem.Companion.getCOLUMNS(), null, null, null, null, null);
    }

    public void replaceBudgetDatabase(Cursor cursor) throws SQLException {
        if (cursor.getCount() > 0) {
            SQLiteDatabase db = getWritableDatabase();
            try {
                db.beginTransaction();
                db.delete(Contract.BudgetItem.TABLE, null, null);
                for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                    ContentValues contentValues = cursorRowToContentValues(cursor);
                    db.insert(Contract.BudgetItem.TABLE, null, contentValues);
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
        cursor.close();
    }

    private static ContentValues cursorRowToContentValues(Cursor cursor) {
        ContentValues values = new ContentValues();
        String[] columns = cursor.getColumnNames();
        int length = columns.length;
        for (int i = 0; i < length; i++) {
            switch (cursor.getType(i)) {
                case Cursor.FIELD_TYPE_NULL:
                    values.putNull(columns[i]);
                    break;
                case Cursor.FIELD_TYPE_INTEGER:
                    values.put(columns[i], cursor.getLong(i));
                    break;
                case Cursor.FIELD_TYPE_FLOAT:
                    values.put(columns[i], cursor.getDouble(i));
                    break;
                case Cursor.FIELD_TYPE_STRING:
                    values.put(columns[i], cursor.getString(i));
                    break;
                case Cursor.FIELD_TYPE_BLOB:
                    values.put(columns[i], cursor.getBlob(i));
                    break;
            }
        }
        return values;
    }
}
