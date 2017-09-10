package com.gb.canibuythat

import com.gb.canibuythat.ui.model.BalanceReading
import com.gb.canibuythat.util.PrefsUtil
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferences @Inject
constructor(private val prefsUtil: PrefsUtil) {

    var estimateDate: Date
        get() {
            val estimateDate = prefsUtil.get(PREF_ESTIMATE_DATE, -1L)

            if (estimateDate == -1L) {
                return Date()
            } else {
                return Date(estimateDate)
            }
        }
        set(date) = prefsUtil.put(PREF_ESTIMATE_DATE, date.time)

    var balanceReading: BalanceReading?
        get() = prefsUtil.get(PREF_READING, BalanceReading.CREATOR)
        set(reading) = if (reading != null) {
            prefsUtil.put(PREF_READING, reading)
        } else {
            prefsUtil.remove(PREF_READING)
        }

    companion object {
        private const val PREF_ESTIMATE_DATE = "PREF_ESTIMATE_DATE"
        private const val PREF_READING = "PREF_READING"
    }
}
