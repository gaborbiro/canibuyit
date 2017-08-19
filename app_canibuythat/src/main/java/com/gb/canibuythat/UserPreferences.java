package com.gb.canibuythat;

import com.gb.canibuythat.ui.model.BalanceReading;
import com.gb.canibuythat.util.PrefsUtil;

import java.util.Date;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class UserPreferences {

    PrefsUtil prefsUtil;

    private static final String PREF_ESTIMATE_DATE = "PREF_ESTIMATE_DATE";
    private static final String PREF_READING = "PREF_READING";

    @Inject
    public UserPreferences(PrefsUtil prefsUtil) {
        this.prefsUtil = prefsUtil;
    }

    public void setEstimateDate(Date date) {
        if (date != null) {
            prefsUtil.put(PREF_ESTIMATE_DATE, date.getTime());
        } else {
            prefsUtil.remove(PREF_ESTIMATE_DATE);
        }
    }

    public Date getEstimateDate() {
        long estimateDate = prefsUtil.get(PREF_ESTIMATE_DATE, -1L);

        if (estimateDate == -1L) {
            return null;
        } else {
            return new Date(estimateDate);
        }
    }

    public void setBalanceReading(BalanceReading reading) {
        if (reading != null) {
            prefsUtil.put(PREF_READING, reading);
        } else {
            prefsUtil.remove(PREF_READING);
        }
    }

    public BalanceReading getBalanceReading() {
        return prefsUtil.get(PREF_READING, BalanceReading.CREATOR);
    }
}
