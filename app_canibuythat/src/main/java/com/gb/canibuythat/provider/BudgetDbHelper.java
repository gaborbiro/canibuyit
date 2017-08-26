package com.gb.canibuythat.provider;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteQueryBuilder;

import com.gb.canibuythat.model.BudgetItem;
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class BudgetDbHelper extends OrmLiteSqliteOpenHelper {

    public static final String DATABASE_NAME = "budget.sqlite";
    private static final int DATABASE_VERSION = 2;

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

    public SQLiteDatabase getDatabaseFromFile(String file) throws SQLiteException {
        return SQLiteDatabase.openDatabase(file, null, 0);
    }

    public Cursor getAllBudgetItems(SQLiteDatabase db) throws SQLException {
        return db.query(Contract.BudgetItem.TABLE, Contract.BudgetItem.COLUMNS, null, null, null, null, null);
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
