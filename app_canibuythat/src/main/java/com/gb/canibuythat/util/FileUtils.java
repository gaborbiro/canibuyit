package com.gb.canibuythat.util;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.channels.FileChannel;
import java.util.Calendar;

import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;

import com.gb.canibuythat.App;


/**
 * Created by GABOR on 2015-jan.-24.
 */
public class FileUtils {

	public static void exportDatabase(String databaseName) {
		String pack = App.getAppContext().getPackageName();

		File sd = Environment.getExternalStorageDirectory();
		File targetFolder = new File(sd + "/CanIBuyThat");

		if (!targetFolder.exists()) {
			targetFolder.mkdirs();
		}
		String targetFilename = "budget-" + Calendar.getInstance().getTime() + ".sqlite";
		File to = new File(targetFolder, targetFilename);

		File data = Environment.getDataDirectory();
		String currentDBPath = "/data/" + pack + "/databases/" + databaseName;
		File from = new File(data, currentDBPath);

		copyFiles(from, to);
	}


	public static void importDatabase(File from, String targetDBName) {
		String pack = App.getAppContext().getPackageName();

		File data = Environment.getDataDirectory();
		String currentDBPath = "/data/" + pack + "/databases/" + targetDBName;
		File to = new File(data, currentDBPath);

		copyFiles(from, to);
	}


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
			String[] projection = {
				"_data"
			};
			Cursor cursor;

			try {
				cursor = App.getAppContext().getContentResolver().query(uri, projection, null, null, null);
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
}
