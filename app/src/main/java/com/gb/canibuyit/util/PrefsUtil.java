package com.gb.canibuyit.util;


import android.content.Context;
import android.content.SharedPreferences;
import android.os.Parcel;
import android.os.Parcelable;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Base64;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Singleton;

@SuppressWarnings({"SameParameterValue", "unused"})
@Singleton
public class PrefsUtil {

    private static final String PREFS_NAME = "settings";
    private static final String SEPARATOR = "dfg,hsdfk__jg34n95t";

    private Context appContext;
    private SecurePreferences securePreferences;

    @Inject
    public PrefsUtil(Context appContext) {
        this.appContext = appContext;
    }

    public void put(String key, Parcelable parcelable) {
        Parcel parcel = Parcel.obtain();
        parcelable.writeToParcel(parcel, 0);
        byte[] bytes = parcel.marshall();
        parcel.recycle();
        put(key, Base64.encodeToString(bytes, 0));
    }


    public <T> T get(String key, Parcelable.Creator<T> creator) {
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


    public Map get(String key, Map defaultValues) {
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


    public void put(String key, Map map) {
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

    public String[] get(String key, String[] defaultValues) {
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

    public void put(String key, String[] values) {
        put(key, TextUtils.join(SEPARATOR, values));
    }

    public void put(String key, boolean value) {
        getSecurePreferences().put(key, Boolean.toString(value));
    }

    public boolean get(String key, boolean defaultValue) {
        String value = getSecurePreferences().getString(key);
        return TextUtils.isEmpty(value) ? defaultValue : Boolean.valueOf(value);
    }

    public void put(String key, String value) {
        getSecurePreferences().put(key, value);
    }

    public String get(String key, String defaultValue) {
        String value = getSecurePreferences().getString(key);
        return TextUtils.isEmpty(value) ? defaultValue : value;
    }

    public void put(String key, int value) {
        getSecurePreferences().put(key, Integer.toString(value));
    }

    public int get(String key, int defaultValue) {
        String value = getSecurePreferences().getString(key);
        return TextUtils.isEmpty(value) ? defaultValue : Integer.valueOf(value);
    }

    public void put(String key, long value) {
        getSecurePreferences().put(key, Long.toString(value));
    }

    public long get(String key, long defaultValue) {
        String value = getSecurePreferences().getString(key);
        return TextUtils.isEmpty(value) ? defaultValue : Long.valueOf(value);
    }

    public void put(String key, float value) {
        getSecurePreferences().put(key, Float.toString(value));
    }

    public float get(String key, float defaultValue) {
        String value = getSecurePreferences().getString(key);
        return TextUtils.isEmpty(value) ? defaultValue : Float.valueOf(value);
    }

    public void remove(String key) {
        getSecurePreferences().removeValue(key);
    }

    public void clear() {
        getSecurePreferences().clear();
    }

    private SecurePreferences getSecurePreferences() {
        if (securePreferences == null) {
            securePreferences = new SecurePreferences(appContext, PREFS_NAME, generateUDID(), true);
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
    public void registerOnSharedPreferenceChangeListener(String key, SharedPreferences.OnSharedPreferenceChangeListener listener) {
        getSecurePreferences().registerOnSharedPreferenceChangeListener(key, listener);
    }

    /**
     * Unregisters a previous callback.
     *
     * @param key PReference key, the callback of which that should be unregistered.
     * @see #registerOnSharedPreferenceChangeListener
     */
    public void unregisterOnSharedPreferenceChangeListener(String key) {
        getSecurePreferences().unregisterOnSharedPreferenceChangeListener(key);
    }

    /**
     * Generate a unique id for the device. Changes with every factory reset. If the
     * device doesn't have a proper
     * android_id and deviceId, it falls back to a randomly generated id, that is
     * persisted in SharedPreferences.
     */
    private String generateUDID() {
        String deviceId = null;
        String androidId;
        UUID deviceUuid = null;

        // androidId changes with every factory reset (which is useful in our case)
        androidId = "" + android.provider.Settings.Secure.getString(appContext.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);

        try {
            if (!"9774d56d682e549c".equals(androidId)) {
                deviceUuid = UUID.nameUUIDFromBytes(androidId.getBytes("utf8"));
            } else {
                // On some 2.2 devices androidId is always 9774d56d682e549c,
                // which is unsafe
                TelephonyManager tm = (TelephonyManager) appContext.getSystemService(Context.TELEPHONY_SERVICE);
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
