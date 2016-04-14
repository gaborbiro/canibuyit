package com.gb.canibuythat.util;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.text.TextUtils;
import android.widget.Toast;

import com.gb.canibuythat.App;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DBUtils {

    public static void exportDatabase(String databaseName) {
        String pack = App.getAppContext()
                .getPackageName();

        File sd = Environment.getExternalStorageDirectory();
        File targetFolder = new File(sd + "/CanIBuyThat");

        if (!targetFolder.exists()) {
            targetFolder.mkdirs();
        }
        String targetFilename = "budget-" + Calendar.getInstance()
                .getTime() + ".sqlite";
        File to = new File(targetFolder, targetFilename);

        File data = Environment.getDataDirectory();
        String currentDBPath = "/data/" + pack + "/databases/" + databaseName;
        File from = new File(data, currentDBPath);

        FileUtils.copyFiles(from, to);
    }


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
                db.endTransaction();
            } catch (android.database.SQLException e) {
                e.printStackTrace();
                Toast.makeText(App.getAppContext(), "Error importing database",
                        Toast.LENGTH_SHORT)
                        .show();
            }
        } else {
            Toast.makeText(App.getAppContext(), "Error importing database",
                    Toast.LENGTH_SHORT)
                    .show();
        }
    }

    public static String[] getSQLScriptForTable(String path, String table,
            String[] columns) {
        ArrayList<String> commandLine = new ArrayList<String>();
        commandLine.add("sqlite3");
        commandLine.add(path);
        commandLine.add(".dump");
        try {
            Process process = Runtime.getRuntime()
                    .exec(commandLine.toArray(new String[commandLine.size()]));
            BufferedReader bufferedReader =
                    new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuffer sqlScript = new StringBuffer();
            String line;

            while (!TextUtils.isEmpty(line = bufferedReader.readLine())) {
                sqlScript.append(line);
                sqlScript.append("\n");
            }
            bufferedReader.close();

            String[] commands = sqlScript.toString()
                    .split(";");
            List<String> result = new ArrayList<String>();
            String insertPrefix = "\nINSERT INTO \"" + table + "\"";
            String createPrefix = "\nCREATE TABLE `" + table + "`";
            String columnSpec = null;

            for (String command : commands) {
                if (command.startsWith(createPrefix)) {
                    // this should happen first
                    columnSpec = generateColumnSpecFromCommand(command, columns);
                } else if (command.startsWith(insertPrefix)) {
                    result.add(
                            command.replace(insertPrefix, insertPrefix + " " + columnSpec)
                                    .trim());
                }
            }
            return result.toArray(new String[result.size()]);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    private static String generateColumnSpecFromCommand(String command,
            String[] knownColumns) {
        String[] sortedColumns = sortByOccurrence(knownColumns, command);
        String[] quotedColumns = new String[knownColumns.length];
        for (int i = 0; i < sortedColumns.length; i++) {
            quotedColumns[i] = "`" + sortedColumns[i] + "`";
        }
        return "(" + TextUtils.join(", ", quotedColumns) + ")";
    }

    /**
     * It is assumed, that each item in the specified <code>array</code> appears once
     * or not at all in the specified <code>text</code>.<br>
     * The items of the specified <code>array</code> are sorted based on the order in
     * which they occur in the specified <code>text</code>.
     */
    public static String[] sortByOccurrence(String[] array, String text) {
        final Map<String, Integer> indexMap = new HashMap<String, Integer>();

        for (int i = 0; i < array.length; i++) {
            indexMap.put(array[i], text.indexOf(array[i]));
        }
        Arrays.sort(array, new Comparator<String>() {
            @Override
            public int compare(String lhs, String rhs) {
                return indexMap.get(lhs)
                        .compareTo(indexMap.get(rhs));
            }
        });
        return array;
    }
}
