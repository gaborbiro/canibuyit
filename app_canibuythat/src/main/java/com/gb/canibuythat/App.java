package com.gb.canibuythat;


import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.gb.canibuythat.provider.BudgetModifierDbHelper;
import com.j256.ormlite.android.apptools.OpenHelperManager;


/**
 * Created by GABOR on 2015-jan.-24.
 */
public class App extends Application {

    private static final String TAG = "App";

    private static Context appContext;

    private Activity firstActivity;

    public App() {
        this.appContext = this;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(new ActivityWatcher());
    }


    public static Context getAppContext() {
        return appContext;
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
                BudgetModifierDbHelper.get().cleanup();
                firstActivity = null;
            }
        }
    }
}
