package com.gb.canibuythat.util;

import android.util.Log;

public class Logger {

    private static final String TAG = "CanIBuyThat";

    public static void d(String tag, String message) {
        Log.d(TAG, "(" + tag + ") " + message);
    }

    public static void d(String tag, Throwable t) {
        Log.d(TAG, "(" + tag + ")", t);
    }

    public static void e(String tag, String message) {
        Log.e(TAG, "(" + tag + ") " + message);
    }

    public static void e(String tag, Throwable t) {
        Log.e(TAG, "(" + tag + ")", t);
    }

    public static void v(String tag, String message) {
        Log.v(TAG, "(" + tag + ") " + message);
    }
}
