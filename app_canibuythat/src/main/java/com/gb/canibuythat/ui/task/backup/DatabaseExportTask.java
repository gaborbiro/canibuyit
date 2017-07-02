package com.gb.canibuythat.ui.task.backup;

import android.os.AsyncTask;
import android.os.Environment;

import com.gb.canibuythat.App;
import com.gb.canibuythat.util.FileUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DatabaseExportTask extends AsyncTask<Void, Void, String> {

    private String databaseName;

    public DatabaseExportTask(String databaseName) {
        this.databaseName = databaseName;
    }

    @Override
    protected String doInBackground(Void... params) {
        String pack = App.getAppContext().getPackageName();

        File sd = Environment.getExternalStorageDirectory();
        File targetFolder = new File(sd + "/CanIBuyThat");

        if (!targetFolder.exists()) {
            targetFolder.mkdirs();
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd'T'HHmmssZ");
        String targetFilename = "budget-" + sdf.format(new Date()) + ".sqlite";
        File to = new File(targetFolder, targetFilename);

        File data = Environment.getDataDirectory();
        String currentDBPath = "/data/" + pack + "/databases/" + databaseName;
        File from = new File(data, currentDBPath);

        FileUtils.copyFiles(from, to);
        return to.getPath();
    }
}
