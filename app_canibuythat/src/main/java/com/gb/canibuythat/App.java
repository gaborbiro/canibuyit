package com.gb.canibuythat;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.gb.canibuythat.provider.BudgetDbHelper;

public class App extends Application {

    private static final String TAG = "App";

    private static Context appContext;

    private Activity firstActivity;

    public App() {
        this.appContext = this;
    }

    public static Context getAppContext() {
        return appContext;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(new ActivityWatcher());
    }

    private class ActivityWatcher extends ActivityLifecycleCallbacksAdapter {

        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            super.onActivityCreated(activity, savedInstanceState);
            if (firstActivity == null) {
                firstActivity = activity;
            }
        }

        @Override
        public void onActivityDestroyed(Activity activity) {
            if (firstActivity == activity) {
                Log.d(TAG, "Releasing sqlite connection");
                BudgetDbHelper.get().cleanup();
                firstActivity = null;
            }
        }
    }
}
