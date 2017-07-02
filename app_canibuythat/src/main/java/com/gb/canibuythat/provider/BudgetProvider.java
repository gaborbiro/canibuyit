package com.gb.canibuythat.provider;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;

import java.util.Arrays;
import java.util.HashSet;

public class BudgetProvider extends ContentProvider {

    public static final String AUTHORITY = "com.gb.canibuythat.authority.budget";

    public static final Uri BASE_CONTENT_URI =
            new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT)
                    .authority(AUTHORITY)
                    .build();

    public static final int ID_BUDGET_ITEMS = 0;
    public static final String PATH_BUDGET_ITEMS = "budget_items";
    public static final Uri BUDGET_ITEMS_URI = BASE_CONTENT_URI.buildUpon()
            .appendPath(PATH_BUDGET_ITEMS)
            .build();

    public static final int ID_BUDGET_ITEM = 1;
    public static final String PATH_BUDGET_ITEM = "budget_items/#";
    public static final Uri BUDGET_ITEM_URI = BASE_CONTENT_URI.buildUpon()
            .appendPath(PATH_BUDGET_ITEM)
            .build();

    private BudgetDbHelper dbHelper;

    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        uriMatcher.addURI(AUTHORITY, PATH_BUDGET_ITEMS, ID_BUDGET_ITEMS);
        uriMatcher.addURI(AUTHORITY, PATH_BUDGET_ITEM, ID_BUDGET_ITEM);
    }

    @Override
    public boolean onCreate() {
        dbHelper = BudgetDbHelper.get();
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        // Using SQLiteQueryBuilder instead of query() method
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

        // Set the table
        queryBuilder.setTables(Contract.BudgetItem.TABLE);

        int uriType = uriMatcher.match(uri);
        switch (uriType) {
            case ID_BUDGET_ITEMS:
                checkColumns(Contract.BudgetItem.class, projection);
                break;
            case ID_BUDGET_ITEM:
                checkColumns(Contract.BudgetItem.class, projection);
                // adding the ID to the original query
                queryBuilder.appendWhere(Contract.BudgetItem._ID + "=" + uri.getLastPathSegment());
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Cursor cursor = queryBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder);
        // make sure that potential listeners are getting notified
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        int uriType = uriMatcher.match(uri);
        SQLiteDatabase sqlDB = dbHelper.getWritableDatabase();
        long id;
        switch (uriType) {
            case ID_BUDGET_ITEMS:
                id = sqlDB.insert(Contract.BudgetItem.TABLE, null, values);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return Uri.parse(PATH_BUDGET_ITEMS + "/" + id);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int uriType = uriMatcher.match(uri);
        SQLiteDatabase sqlDB = dbHelper.getWritableDatabase();
        int rowsDeleted;
        switch (uriType) {
            case ID_BUDGET_ITEMS:
                rowsDeleted = sqlDB.delete(Contract.BudgetItem.TABLE, selection, selectionArgs);
                break;
            case ID_BUDGET_ITEM:
                String id = uri.getLastPathSegment();

                if (TextUtils.isEmpty(selection)) {
                    rowsDeleted = sqlDB.delete(Contract.BudgetItem.TABLE, Contract.BudgetItem._ID + "=" + id, null);
                } else {
                    rowsDeleted = sqlDB.delete(Contract.BudgetItem.TABLE, Contract.BudgetItem._ID + "=" + id + " and " + selection, selectionArgs);
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return rowsDeleted;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int uriType = uriMatcher.match(uri);
        SQLiteDatabase sqlDB = dbHelper.getWritableDatabase();
        int rowsUpdated = 0;
        switch (uriType) {
            case ID_BUDGET_ITEMS:
                rowsUpdated = sqlDB.update(Contract.BudgetItem.TABLE, values, selection, selectionArgs);
                break;
            case ID_BUDGET_ITEM:
                String id = uri.getLastPathSegment();

                if (TextUtils.isEmpty(selection)) {
                    rowsUpdated = sqlDB.update(Contract.BudgetItem.TABLE, values, Contract.BudgetItem._ID + "=" + id, null);
                } else {
                    rowsUpdated = sqlDB.update(Contract.BudgetItem.TABLE, values, Contract.BudgetItem._ID + "=" + id + " and " + selection, selectionArgs);
                }
                break;
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return rowsUpdated;
    }

    private void checkColumns(Class<? extends BaseColumns> tableClass, String[] projection) {
        String[] available;
        try {
            available = (String[]) tableClass.getDeclaredField("COLUMNS").get(null);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new IllegalArgumentException("The specified table class does not have public static field COLUMNS");
        }

        if (projection != null) {
            HashSet<String> requestedColumns = new HashSet<>(Arrays.asList(projection));
            HashSet<String> availableColumns = new HashSet<>(Arrays.asList(available));
            // check if all columns which are requested are available
            if (!availableColumns.containsAll(requestedColumns)) {
                throw new IllegalArgumentException("Unknown columns in projection");
            }
        }
    }
}
