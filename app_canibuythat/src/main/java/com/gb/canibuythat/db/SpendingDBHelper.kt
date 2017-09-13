package com.gb.canibuythat.db


import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import com.gb.canibuythat.model.Project
import com.gb.canibuythat.model.Spending
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper
import com.j256.ormlite.support.ConnectionSource
import com.j256.ormlite.table.TableUtils
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.ObjectInputStream
import java.sql.SQLException
import java.util.regex.Pattern
import javax.inject.Inject

class SpendingDBHelper @Inject
constructor(appContext: Context) : OrmLiteSqliteOpenHelper(appContext, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(database: SQLiteDatabase, connectionSource: ConnectionSource) {
        try {
            TableUtils.createTable(connectionSource, Spending::class.java)
            TableUtils.createTable(connectionSource, Project::class.java)
        } catch (e: SQLException) {
            e.printStackTrace()
        }
    }

    override fun onUpgrade(database: SQLiteDatabase, connectionSource: ConnectionSource, oldVersion: Int, newVersion: Int) {}

    @Throws(SQLiteException::class)
    fun getDatabaseFromFile(file: String): SQLiteDatabase {
        return SQLiteDatabase.openDatabase(file, null, 0)
    }

    @Throws(SQLException::class)
    fun getAllSpendings(db: SQLiteDatabase): Cursor {
        return db.query(Contract.Spending.TABLE, Contract.Spending.COLUMNS, null, null, null, null, null)
    }

    @Throws(SQLException::class, IOException::class, ClassNotFoundException::class)
    fun insertSpendings(cursor: Cursor, filter: Map<String, Any?>?) {
        if (cursor.count > 0) {
            val db = writableDatabase
            try {
                db.beginTransaction()
                cursor.moveToFirst()
                while (!cursor.isAfterLast) {
                    val contentValues = cursorRowToContentValues(cursor)
                    if (filter == null || match(contentValues, filter)) {
                        db.insert(Contract.Spending.TABLE, null, contentValues)
                    }
                    cursor.moveToNext()
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }
        cursor.close()
    }

    @Throws(IOException::class, ClassNotFoundException::class)
    private fun match(contentValues: ContentValues, filter: Map<String, Any?>): Boolean {
        for (key in filter.keys) {
            if (!match(getContentValue(contentValues, key), filter[key])) {
                return false
            }
        }
        return true
    }

    @Throws(IOException::class, ClassNotFoundException::class)
    private fun getContentValue(contentValues: ContentValues, key: String): Any? {
        if (contentValues.containsKey(key)) {
            return contentValues.get(key)
        } else if (key.contains("/")) {
            val m = Pattern.compile("/").matcher(key)
            if (m.find()) {
                val o = contentValues.get(key.substring(0, m.start()))
                return getContentValue(o, key.substring(m.end(), key.length))
            }
        }
        return null
    }

    @Throws(IOException::class, ClassNotFoundException::class)
    private fun getContentValue(contentValues: Any, key: String): Any? {
        var cv = contentValues
        if (cv is ContentValues) {
            return getContentValue(cv, key)
        } else if (cv is Map<*, *>) {
            return cv[key]
        } else if (cv is ByteArray) {
            cv = ObjectInputStream(ByteArrayInputStream(cv)).readObject()
            return getContentValue(cv, key)
        } else {
            return null
        }
    }

    private fun match(actual: Any?, expected: Any?): Boolean {
        if (expected == null) {
            return actual == null
        } else if (expected.javaClass == Any::class.java) {
            return actual != null
        } else {
            return expected == actual
        }
    }

    companion object {

        val DATABASE_NAME = "spendings.sqlite"
        private val DATABASE_VERSION = 1

        private fun cursorRowToContentValues(cursor: Cursor): ContentValues {
            val values = ContentValues()
            val columns = cursor.columnNames
            val length = columns.size
            for (i in 0..length - 1) {
                when (cursor.getType(i)) {
                    Cursor.FIELD_TYPE_NULL -> values.putNull(columns[i])
                    Cursor.FIELD_TYPE_INTEGER -> values.put(columns[i], cursor.getLong(i))
                    Cursor.FIELD_TYPE_FLOAT -> values.put(columns[i], cursor.getDouble(i))
                    Cursor.FIELD_TYPE_STRING -> values.put(columns[i], cursor.getString(i))
                    Cursor.FIELD_TYPE_BLOB -> values.put(columns[i], cursor.getBlob(i))
                }
            }
            values.remove(Contract.Spending._ID)
            return values
        }

        val FILTER_MONZO = object : HashMap<String, Any?>() {
            init {
                put(Contract.Spending.SOURCE_DATA + "/" + Spending.SOURCE_MONZO_CATEGORY, Any())
            }
        }

        val FILTER_NON_MONZO = object : HashMap<String, Any?>() {
            init {
                put(Contract.Spending.SOURCE_DATA + "/" + Spending.SOURCE_MONZO_CATEGORY, null)
            }
        }
    }
}
