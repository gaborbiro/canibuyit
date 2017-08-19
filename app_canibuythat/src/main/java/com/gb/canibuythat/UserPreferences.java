package com.gb.canibuythat;

import com.gb.canibuythat.ui.model.BalanceReading;
import com.gb.canibuythat.util.PrefsUtil;

import java.util.Date;

public class UserPreferences {

    private static final String PREF_ESTIMATE_DATE = "PREF_ESTIMATE_DATE";
    private static final String PREF_READING = "PREF_READING";

    public static void setEstimateDate(Date date) {
        if (date != null) {
            PrefsUtil.put(PREF_ESTIMATE_DATE, date.getTime());
        } else {
            PrefsUtil.remove(PREF_ESTIMATE_DATE);
        }
    }

    public static Date getEstimateDate() {
        long estimateDate = PrefsUtil.get(PREF_ESTIMATE_DATE, -1L);

        if (estimateDate == -1L) {
            return null;
        } else {
            return new Date(estimateDate);
        }
    }

    public static void setBalanceReading(BalanceReading reading) {
        if (reading != null) {
            PrefsUtil.put(PREF_READING, reading);
        } else {
            PrefsUtil.remove(PREF_READING);
        }
    }

    public static BalanceReading getBalanceReading() {
        return PrefsUtil.get(PREF_READING, BalanceReading.CREATOR);
    }
}
