package com.gb.canibuyit.feature.spending.persistence

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import com.gb.canibuyit.feature.monzo.MONZO_CATEGORY
import com.gb.canibuyit.feature.project.model.ApiProject
import com.gb.canibuyit.feature.spending.persistence.model.ApiCycleSpending
import com.gb.canibuyit.feature.spending.persistence.model.ApiSaving
import com.gb.canibuyit.feature.spending.persistence.model.ApiSpending
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
constructor(appContext: Context) :
    OrmLiteSqliteOpenHelper(appContext, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(database: SQLiteDatabase, connectionSource: ConnectionSource) {
        try {
            TableUtils.createTable(connectionSource, ApiSpending::class.java)
            TableUtils.createTable(connectionSource, ApiProject::class.java)
            TableUtils.createTable(connectionSource, ApiSaving::class.java)
            TableUtils.createTable(connectionSource, ApiCycleSpending::class.java)
        } catch (e: SQLException) {
            e.printStackTrace()
        }
    }

    override fun onUpgrade(database: SQLiteDatabase, connectionSource: ConnectionSource,
                           oldVersion: Int, newVersion: Int) {
    }

    @Throws(SQLiteException::class)
    fun getDatabaseFromFile(file: String): SQLiteDatabase {
        return SQLiteDatabase.openDatabase(file, null, 0)
    }

    @Throws(SQLException::class)
    fun getAllSpendings(db: SQLiteDatabase): Cursor {
        return db.query(Contract.Spending.TABLE, Contract.Spending.COLUMNS, null, null, null, null,
            null)
    }

    @Throws(SQLException::class, IOException::class, ClassNotFoundException::class)
    fun insertSpendings(cursor: Cursor, filter: Map<String, Any?>?) {
        if (cursor.count > 0) {
            writableDatabase.apply {
                try {
                    beginTransaction()
                    cursor.moveToFirst()
                    while (!cursor.isAfterLast) {
                        val contentValues = cursorRowToContentValues(cursor)
                        if (filter == null || match(contentValues, filter)) {
                            insert(Contract.Spending.TABLE, null, contentValues)
                        }
                        cursor.moveToNext()
                    }
                    setTransactionSuccessful()
                } finally {
                    endTransaction()
                }
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
            val matcher = Pattern.compile("/").matcher(key)
            if (matcher.find()) {
                val o = contentValues.get(key.substring(0, matcher.start()))
                return getContentValue(o, key.substring(matcher.end(), key.length))
            }
        }
        return null
    }

    @Throws(IOException::class, ClassNotFoundException::class)
    private fun getContentValue(contentValues: Any, key: String): Any? {
        return when (contentValues) {
            is ContentValues -> getContentValue(contentValues, key)
            is Map<*, *> -> contentValues[key]
            is ByteArray -> {
                ObjectInputStream(ByteArrayInputStream(contentValues)).readObject().apply {
                    getContentValue(this, key)
                }
            }
            else -> null
        }
    }

    private fun match(actual: Any?, expected: Any?): Boolean {
        return when {
            expected == null -> actual == null
            expected.javaClass == Any::class.java -> actual != null
            else -> expected == actual
        }
    }

    private fun cursorRowToContentValues(cursor: Cursor): ContentValues {
        val values = ContentValues()
        val columns = cursor.columnNames
        val length = columns.size
        for (i in 0 until length) {
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
}

const val DATABASE_NAME = "spendings.sqlite"

val FILTER_MONZO = object : HashMap<String, Any?>() {
    init {
        put(Contract.Spending.SOURCE_DATA + "/" + MONZO_CATEGORY, Any())
    }
}

val FILTER_NON_MONZO = object : HashMap<String, Any?>() {
    init {
        put(Contract.Spending.SOURCE_DATA + "/" + MONZO_CATEGORY, null)
    }
}

private const val DATABASE_VERSION = 1
