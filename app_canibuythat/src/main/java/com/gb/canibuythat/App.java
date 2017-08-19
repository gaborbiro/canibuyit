package com.gb.canibuythat;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.gb.canibuythat.di.Injector;
import com.gb.canibuythat.provider.BudgetDbHelper;
import com.gb.canibuythat.util.Logger;

import java.io.UnsupportedEncodingException;
import java.util.UUID;

public class App extends Application {

    private static final String TAG = "App";

    private static Context appContext;

    private Activity firstActivity;

    public App() {
        appContext = this;
    }

    public static Context getAppContext() {
        return appContext;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(new ActivityWatcher());

        Injector.INSTANCE.initializeCanIBuyThatComponent(this);
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
                Logger.d(TAG, "Releasing sqlite connection");
                BudgetDbHelper.get().cleanup();
                firstActivity = null;
            }
        }
    }

    /**
     * Generate a unique id for the device. Changes with every factory reset. If the
     * device doesn't have a proper
     * android_id and deviceId, it falls back to a randomly generated id, that is
     * persisted in SharedPreferences.
     */
    public static String generateUDID() {
        String deviceId = null;
        String androidId = null;
        UUID deviceUuid = null;

        // androidId changes with every factory reset (which is useful in our case)
        androidId = "" + android.provider.Settings.Secure.getString(getAppContext().getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);

        try {
            if (!"9774d56d682e549c".equals(androidId)) {
                deviceUuid = UUID.nameUUIDFromBytes(androidId.getBytes("utf8"));
            } else {
                // On some 2.2 devices androidId is always 9774d56d682e549c,
                // which is unsafe
                TelephonyManager tm = (TelephonyManager) getAppContext().getSystemService(Context.TELEPHONY_SERVICE);
                if (tm != null) {
                    // Tablets may not have imei and/or imsi.
                    // Does not change on factory reset.
                    deviceId = tm.getDeviceId();
                }
                if (TextUtils.isEmpty(deviceId)) {
                    // worst case scenario as this id is lost when the
                    // application stops
                    deviceUuid = UUID.randomUUID();
                } else {
                    deviceUuid = UUID.nameUUIDFromBytes(deviceId.getBytes("utf8"));
                }
            }
        } catch (UnsupportedEncodingException e) {
            // Change it back to "utf8" right now!!
        }
        return deviceUuid.toString();
    }
}
