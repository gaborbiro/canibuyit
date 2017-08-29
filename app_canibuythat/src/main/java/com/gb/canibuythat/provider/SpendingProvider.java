package com.gb.canibuythat.provider;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import com.gb.canibuythat.di.Injector;

import java.util.Arrays;
import java.util.HashSet;

import javax.inject.Inject;

public class SpendingProvider extends ContentProvider {

    public static final String AUTHORITY = "com.gb.canibuythat.authority.spending";

    public static final Uri BASE_CONTENT_URI =
            new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT)
                    .authority(AUTHORITY)
                    .build();

    public static final int ID_SPENDINGS = 0;
    public static final String PATH_SPENDINGS = "spendings";
    public static final Uri SPENDINGS_URI = BASE_CONTENT_URI.buildUpon()
            .appendPath(PATH_SPENDINGS)
            .build();

    public static final int ID_SPENDING = 1;
    public static final String PATH_SPENDING = "spendings/#";
    public static final Uri SPENDING_URI = BASE_CONTENT_URI.buildUpon()
            .appendPath(PATH_SPENDING)
            .build();

    @Inject SpendingDbHelper dbHelper;

    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        uriMatcher.addURI(AUTHORITY, PATH_SPENDINGS, ID_SPENDINGS);
        uriMatcher.addURI(AUTHORITY, PATH_SPENDING, ID_SPENDING);
    }

    @Override
    public boolean onCreate() {
        Injector.INSTANCE.getGraph().inject(this);
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        // Using SQLiteQueryBuilder instead of query() method
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

        // Set the table
        queryBuilder.setTables(Contract.Spending.TABLE);

        int uriType = uriMatcher.match(uri);
        switch (uriType) {
            case ID_SPENDINGS:
                checkColumns(Contract.Spending.Companion.getCOLUMNS(), projection);
                break;
            case ID_SPENDING:
                checkColumns(Contract.Spending.Companion.getCOLUMNS(), projection);
                // adding the ID to the original query
                queryBuilder.appendWhere(Contract.Spending._ID + "=" + uri.getLastPathSegment());
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
            case ID_SPENDINGS:
                id = sqlDB.insert(Contract.Spending.TABLE, null, values);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return Uri.parse(PATH_SPENDINGS + "/" + id);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int uriType = uriMatcher.match(uri);
        SQLiteDatabase sqlDB = dbHelper.getWritableDatabase();
        int rowsDeleted;
        switch (uriType) {
            case ID_SPENDINGS:
                rowsDeleted = sqlDB.delete(Contract.Spending.TABLE, selection, selectionArgs);
                break;
            case ID_SPENDING:
                String id = uri.getLastPathSegment();

                if (TextUtils.isEmpty(selection)) {
                    rowsDeleted = sqlDB.delete(Contract.Spending.TABLE, Contract.Spending._ID + "=" + id, null);
                } else {
                    rowsDeleted = sqlDB.delete(Contract.Spending.TABLE, Contract.Spending._ID + "=" + id + " and " + selection, selectionArgs);
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
            case ID_SPENDINGS:
                rowsUpdated = sqlDB.update(Contract.Spending.TABLE, values, selection, selectionArgs);
                break;
            case ID_SPENDING:
                String id = uri.getLastPathSegment();

                if (TextUtils.isEmpty(selection)) {
                    rowsUpdated = sqlDB.update(Contract.Spending.TABLE, values, Contract.Spending._ID + "=" + id, null);
                } else {
                    rowsUpdated = sqlDB.update(Contract.Spending.TABLE, values, Contract.Spending._ID + "=" + id + " and " + selection, selectionArgs);
                }
                break;
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return rowsUpdated;
    }

    private void checkColumns(String[] columns, String[] projection) {
        if (projection != null) {
            HashSet<String> requestedColumns = new HashSet<>(Arrays.asList(projection));
            HashSet<String> availableColumns = new HashSet<>(Arrays.asList(columns));
            // check if all columns which are requested are available
            if (!availableColumns.containsAll(requestedColumns)) {
                throw new IllegalArgumentException("Unknown columns in projection");
            }
        }
    }
}
