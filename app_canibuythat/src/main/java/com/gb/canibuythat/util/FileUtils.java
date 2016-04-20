package com.gb.canibuythat.util;

import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import com.gb.canibuythat.App;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.channels.FileChannel;

public class FileUtils {

    public static void copyFiles(File from, File to) {
        FileChannel src = null;
        FileChannel dst = null;
        try {
            src = new FileInputStream(from).getChannel();
            dst = new FileOutputStream(to).getChannel();
            dst.transferFrom(src, 0, src.size());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                src.close();
                dst.close();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }


    public static String getPath(Uri uri) throws URISyntaxException {
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            String[] projection = {"_data"};
            Cursor cursor;

            try {
                cursor = App.getAppContext()
                        .getContentResolver()
                        .query(uri, projection, null, null, null);
                int column_index = cursor.getColumnIndexOrThrow("_data");
                if (cursor.moveToFirst()) {
                    return cursor.getString(column_index);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    public static String streamToString(InputStream is) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is));
        StringBuffer result = new StringBuffer();
        String line;

        while (!TextUtils.isEmpty(line = bufferedReader.readLine())) {
            result.append(line);
            result.append("\n");
        }
        bufferedReader.close();
        return result.toString();
    }
}
