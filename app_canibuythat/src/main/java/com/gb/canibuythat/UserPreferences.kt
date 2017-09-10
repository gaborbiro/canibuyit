package com.gb.canibuythat

import android.content.SharedPreferences
import com.gb.canibuythat.ui.model.BalanceReading
import com.gb.canibuythat.util.PrefsUtil
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferences @Inject
constructor(private val prefsUtil: PrefsUtil) : SharedPreferences.OnSharedPreferenceChangeListener {

    private val estimateDateSubject: PublishSubject<Date> = PublishSubject.create()
    private val balanceReadingSubject: PublishSubject<BalanceReading?> = PublishSubject.create()

    init {
        prefsUtil.registerOnSharedPreferenceChangeListener(PREF_ESTIMATE_DATE, this)
        prefsUtil.registerOnSharedPreferenceChangeListener(PREF_READING, this)
    }

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

    fun getEstimateDateDataStream(): Observable<Date> {
        return estimateDateSubject
    }

    fun getBalanceReadingDataStream(): Observable<BalanceReading?> {
        return balanceReadingSubject
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            PREF_ESTIMATE_DATE -> estimateDateSubject.onNext(estimateDate)
            PREF_READING -> balanceReadingSubject.onNext(balanceReading ?: BalanceReading(null, 0f))
        }
    }

    companion object {
        private const val PREF_ESTIMATE_DATE = "PREF_ESTIMATE_DATE"
        private const val PREF_READING = "PREF_READING"
    }
}
