package com.gb.canibuythat.util;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.File;

public class DBUtils {

    /**
     * Heavy operations ahead! Do not invoke from UI thread.
     *
     * @param from      Sqlite file to import from. It is assumed the the database
     *                  within contains a table with the specified <code>tableName</code>
     * @param tableName Name of the table to be imported
     * @param columns   Columns of the table to be imported. This needs to be specified
     *                  because otherwise we would need to parse sql script.
     * @param dbHelper  SQLiteOpenHelper instance into which we should import. It is
     *                  assumed that it has a table with the specified
     *                  <code>tableName</code> and specified <code>columns</code>
     */
    public static void importDatabase(File from, String tableName, String[] columns, SQLiteOpenHelper dbHelper) {
        Cursor c = SQLiteDatabase.openOrCreateDatabase(from, null).query(tableName, columns, null, null, null, null, null);

        if (c.getCount() > 0) {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            try {
                db.beginTransaction();
                db.delete(tableName, null, null);
                for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
                    ContentValues contentValues = cursorRowToContentValues(c);
                    db.insert(tableName, null, contentValues);
                }
                db.setTransactionSuccessful();
            } catch (android.database.SQLException e) {
                e.printStackTrace();
            } finally {
                db.endTransaction();
            }
        }
        c.close();
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
