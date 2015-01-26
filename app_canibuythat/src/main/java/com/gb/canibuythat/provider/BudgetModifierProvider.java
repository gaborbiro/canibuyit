package com.gb.canibuythat.provider;


import java.util.Arrays;
import java.util.HashSet;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;


/**
 * Created by gbiro on 1/12/2015.
 */
public class BudgetModifierProvider extends ContentProvider {

	public static final String		AUTHORITY				= "com.gb.canibuythat.authority.budget";

	public static final Uri			BASE_CONTENT_URI		= new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT)
																	.authority(AUTHORITY).build();

	public static final int			ID_BUDGET_MODIFIERS		= 0;
	public static final String		PATH_BUDGET_MODIFIERS	= "budget_modifiers";
	public static final Uri			BUDGET_MODIFIERS_URI	= BASE_CONTENT_URI.buildUpon()
																	.appendPath(PATH_BUDGET_MODIFIERS).build();

	public static final int			ID_BUDGET_MODIFIER		= 1;
	public static final String		PATH_BUDGET_MODIFIER	= "budget_modifiers/#";
	public static final Uri			BUDGET_MODIFIER_URI		= BASE_CONTENT_URI.buildUpon()
																	.appendPath(PATH_BUDGET_MODIFIER).build();

	private BudgetModifierDbHelper	dbHelper;

	private static final UriMatcher	sURIMatcher				= new UriMatcher(UriMatcher.NO_MATCH);
	static {
		sURIMatcher.addURI(AUTHORITY, PATH_BUDGET_MODIFIERS, ID_BUDGET_MODIFIERS);
		sURIMatcher.addURI(AUTHORITY, PATH_BUDGET_MODIFIER, ID_BUDGET_MODIFIER);
	}


	@Override
	public boolean onCreate() {
		dbHelper = new BudgetModifierDbHelper(getContext());
		return true;
	}


	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		// Uisng SQLiteQueryBuilder instead of query() method
		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

		// check if the caller has requested a column which does not exists
		checkColumns(projection);

		// Set the table
		queryBuilder.setTables(Contract.BudgetModifier.TABLE);

		int uriType = sURIMatcher.match(uri);
		switch (uriType) {
		case ID_BUDGET_MODIFIERS:
			break;
		case ID_BUDGET_MODIFIER:
			// adding the ID to the original query
			queryBuilder.appendWhere(Contract.BudgetModifier._ID + "=" + uri.getLastPathSegment());
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
		int uriType = sURIMatcher.match(uri);
		SQLiteDatabase sqlDB = dbHelper.getWritableDatabase();
		long id;
		switch (uriType) {
		case ID_BUDGET_MODIFIERS:
			id = sqlDB.insert(Contract.BudgetModifier.TABLE, null, values);
			break;
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return Uri.parse(PATH_BUDGET_MODIFIERS + "/" + id);
	}


	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		int uriType = sURIMatcher.match(uri);
		SQLiteDatabase sqlDB = dbHelper.getWritableDatabase();
		int rowsDeleted = 0;
		switch (uriType) {
		case ID_BUDGET_MODIFIERS:
			rowsDeleted = sqlDB.delete(Contract.BudgetModifier.TABLE, selection, selectionArgs);
			break;
		case ID_BUDGET_MODIFIER:
			String id = uri.getLastPathSegment();

			if (TextUtils.isEmpty(selection)) {
				rowsDeleted = sqlDB.delete(Contract.BudgetModifier.TABLE, Contract.BudgetModifier._ID + "=" + id, null);
			} else {
				rowsDeleted = sqlDB.delete(Contract.BudgetModifier.TABLE, Contract.BudgetModifier._ID + "=" + id
						+ " and " + selection, selectionArgs);
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
		int uriType = sURIMatcher.match(uri);
		SQLiteDatabase sqlDB = dbHelper.getWritableDatabase();
		int rowsUpdated = 0;
		switch (uriType) {
		case ID_BUDGET_MODIFIERS:
			rowsUpdated = sqlDB.update(Contract.BudgetModifier.TABLE, values, selection, selectionArgs);
			break;
		case ID_BUDGET_MODIFIER:
			String id = uri.getLastPathSegment();
			if (TextUtils.isEmpty(selection)) {
				rowsUpdated = sqlDB.update(Contract.BudgetModifier.TABLE, values, Contract.BudgetModifier._ID + "="
						+ id, null);
			} else {
				rowsUpdated = sqlDB.update(Contract.BudgetModifier.TABLE, values, Contract.BudgetModifier._ID + "="
						+ id + " and " + selection, selectionArgs);
			}
			break;
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return rowsUpdated;
	}


	private void checkColumns(String[] projection) {
		String[] available = {
				Contract.BudgetModifier._ID, Contract.BudgetModifier.TITLE, Contract.BudgetModifier.NOTES,
				Contract.BudgetModifier.AMOUNT, Contract.BudgetModifier.TYPE, Contract.BudgetModifier.LOWER_DATE,
				Contract.BudgetModifier.UPPER_DATE, Contract.BudgetModifier.REPETITION_COUNT,
				Contract.BudgetModifier.PERIOD_MULTIPLIER, Contract.BudgetModifier.PERIOD
		};
		if (projection != null) {
			HashSet<String> requestedColumns = new HashSet<String>(Arrays.asList(projection));
			HashSet<String> availableColumns = new HashSet<String>(Arrays.asList(available));
			// check if all columns which are requested are available
			if (!availableColumns.containsAll(requestedColumns)) {
				throw new IllegalArgumentException("Unknown columns in projection");
			}
		}
	}
}
