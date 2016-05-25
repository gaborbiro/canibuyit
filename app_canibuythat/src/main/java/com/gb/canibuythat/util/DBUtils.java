package com.gb.canibuythat.util;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.text.TextUtils;
import android.widget.Toast;

import com.gb.canibuythat.App;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
    public static void importDatabase(File from, String tableName, String[] columns,
            SQLiteOpenHelper dbHelper) {
        String[] sqlScripts =
                DBUtils.getSQLScriptForTable(from.getPath(), tableName, columns);

        if (sqlScripts != null) {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            try {
                db.beginTransaction();
                db.delete(tableName, null, null);
                for (String sqlScript : sqlScripts) {
                    db.execSQL(sqlScript);
                }
                db.setTransactionSuccessful();
            } catch (android.database.SQLException e) {
                e.printStackTrace();
            } finally {
                db.endTransaction();
            }
        }
    }

    public static String[] getSQLScriptForTable(String path, String table,
            String[] columns) {
        try {
            String[] sql = sqlite3ToSql(path);
            return filterCommandForTable(sql, table, columns);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String[] sqlite3ToSql(String path) throws IOException {
        String[] commandLine = new String[]{"sqlite3", path, ".dump"};
        Process process = Runtime.getRuntime()
                .exec(commandLine);
        return FileUtils.streamToString(process.getInputStream())
                .split(";\n");
    }

    private static String[] filterCommandForTable(String[] sql, String table,
            String[] columns) {
        List<String> result = new ArrayList<>();
        String insertPrefix = "INSERT INTO [\"'`]{1}" + table + "[\"'`]{1}";
        String createPrefix = "CREATE TABLE [\"'`]{1}" + table + "[\"'`]{1}";
        String columnSpec = null;

        for (String command : sql) {
            if (command.matches("^" + createPrefix + ".+")) {
                // this should happen first
                columnSpec = generateColumnSpecFromCommand(command, columns);
            } else if (command.matches("^" + insertPrefix + ".+")) {
                result.add(command.replaceFirst(insertPrefix, "$0 " + columnSpec)
                        .trim());
            }
        }
        return result.toArray(new String[result.size()]);
    }

    private static String generateColumnSpecFromCommand(String command,
            String[] knownColumns) {
        String[] sortedColumns = ArrayUtils.sortByOccurrence(knownColumns, command);
        String[] quotedColumns = new String[knownColumns.length];
        for (int i = 0; i < sortedColumns.length; i++) {
            quotedColumns[i] = "`" + sortedColumns[i] + "`";
        }
        return "(" + TextUtils.join(", ", quotedColumns) + ")";
    }
}
