package com.gb.canibuythat.util;


import android.content.SharedPreferences;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Base64;

import com.gb.canibuythat.App;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;

@SuppressWarnings({"SameParameterValue", "unused"})
public class PrefsUtil {

    private static final String PREFS_NAME = "settings";
    private static final String SEPARATOR = "dfg,hsdfk__jg34n95t";

    private static SecurePreferences securePreferences;


    public static void put(String key, Parcelable parcelable) {
        Parcel parcel = Parcel.obtain();
        parcelable.writeToParcel(parcel, 0);
        byte[] bytes = parcel.marshall();
        parcel.recycle();
        put(key, Base64.encodeToString(bytes, 0));
    }


    public static <T> T get(String key, Parcelable.Creator<T> creator) {
        String data = get(key, (String) null);

        if (data == null) {
            return null;
        }
        byte[] bytes = Base64.decode(data, 0);
        Parcel parcel = Parcel.obtain();
        parcel.unmarshall(bytes, 0, bytes.length);
        parcel.setDataPosition(0);
        return creator.createFromParcel(parcel);
    }


    public static Map get(String key, Map defaultValues) {
        String data = get(key, (String) null);

        if (data == null) {
            return defaultValues;
        }
        try {
            byte[] bytes = Base64.decode(data, 0);
            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes, 0, bytes.length));
            return (Map) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }


    public static void put(String key, Map map) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(baos);
            oos.writeObject(map);
            oos.flush();
            put(key, Base64.encodeToString(baos.toByteArray(), 0));
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                oos.close();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    public static String[] get(String key, String[] defaultValues) {
        String defaultValuesStr;
        if (defaultValues == null) {
            defaultValuesStr = null;
        } else {
            defaultValuesStr = TextUtils.join(SEPARATOR, defaultValues);
        }
        String text = get(key, defaultValuesStr);
        if (!TextUtils.isEmpty(text)) {
            return text.split(SEPARATOR);
        } else {
            return new String[0];
        }
    }

    public static void put(String key, String[] values) {
        put(key, TextUtils.join(SEPARATOR, values));
    }

    public static void put(String key, boolean value) {
        getSecurePreferences().put(key, Boolean.toString(value));
    }

    public static boolean get(String key, boolean defaultValue) {
        String value = getSecurePreferences().getString(key);
        return TextUtils.isEmpty(value) ? defaultValue : Boolean.valueOf(value);
    }

    public static void put(String key, String value) {
        getSecurePreferences().put(key, value);
    }

    public static String get(String key, String defaultValue) {
        String value = getSecurePreferences().getString(key);
        return TextUtils.isEmpty(value) ? defaultValue : value;
    }

    public static void put(String key, int value) {
        getSecurePreferences().put(key, Integer.toString(value));
    }

    public static int get(String key, int defaultValue) {
        String value = getSecurePreferences().getString(key);
        return TextUtils.isEmpty(value) ? defaultValue : Integer.valueOf(value);
    }

    public static void put(String key, long value) {
        getSecurePreferences().put(key, Long.toString(value));
    }

    public static long get(String key, long defaultValue) {
        String value = getSecurePreferences().getString(key);
        return TextUtils.isEmpty(value) ? defaultValue : Long.valueOf(value);
    }

    public static void put(String key, float value) {
        getSecurePreferences().put(key, Float.toString(value));
    }

    public static float get(String key, float defaultValue) {
        String value = getSecurePreferences().getString(key);
        return TextUtils.isEmpty(value) ? defaultValue : Float.valueOf(value);
    }

    public static void remove(String key) {
        getSecurePreferences().removeValue(key);
    }

    private static SecurePreferences getSecurePreferences() {
        if (securePreferences == null) {
            securePreferences = new SecurePreferences(App.getAppContext(), PREFS_NAME, App.generateUDID(), true);
        }
        return securePreferences;
    }

    /**
     * Registers a callback to be invoked when a change happens to the specified preference.
     *
     * @param key      Preference key for which the specified callback should be
     *                 registered to
     * @param listener The callback that will run.
     * @see #unregisterOnSharedPreferenceChangeListener
     */
    public static void registerOnSharedPreferenceChangeListener(String key, SharedPreferences.OnSharedPreferenceChangeListener listener) {
        getSecurePreferences().registerOnSharedPreferenceChangeListener(key, listener);
    }

    /**
     * Unregisters a previous callback.
     *
     * @param key PReference key, the callback of which that should be unregistered.
     * @see #registerOnSharedPreferenceChangeListener
     */
    public static void unregisterOnSharedPreferenceChangeListener(String key) {
        getSecurePreferences().unregisterOnSharedPreferenceChangeListener(key);
    }
}
