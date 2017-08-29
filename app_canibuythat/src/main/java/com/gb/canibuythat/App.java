package com.gb.canibuythat;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.widget.Toast;

import com.gb.canibuythat.di.Injector;
import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.jakewharton.threetenabp.AndroidThreeTen;

public class App extends Application {

    private int activityCount = 0;

    public App() {
        super();
        Injector.INSTANCE.initializeCanIBuyThatComponent(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(new ActivityWatcher());
        AndroidThreeTen.init(this);
    }

    private class ActivityWatcher extends ActivityLifecycleCallbacksAdapter {

        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            super.onActivityCreated(activity, savedInstanceState);
            activityCount++;
        }

        @Override
        public void onActivityDestroyed(Activity activity) {
            activityCount--;
            if (activityCount <= 0) {
                Toast.makeText(App.this, "Releasing sqlite connection", Toast.LENGTH_SHORT).show();
                OpenHelperManager.releaseHelper();
            }
        }
    }
}
