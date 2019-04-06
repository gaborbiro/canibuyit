package com.gb.canibuyit

import android.content.SharedPreferences
import com.gb.canibuyit.feature.spending.model.BalanceReading
import com.gb.lib_prefsutil.PrefsUtil
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferences @Inject
constructor(private val prefsUtil: PrefsUtil) : SharedPreferences.OnSharedPreferenceChangeListener {

    private val estimateDateSubject: PublishSubject<LocalDate> = PublishSubject.create()
    private val balanceReadingSubject: PublishSubject<BalanceReading?> = PublishSubject.create()

    init {
        prefsUtil.registerOnSharedPreferenceChangeListener(PREF_ESTIMATE_DATE, this)
        prefsUtil.registerOnSharedPreferenceChangeListener(PREF_READING, this)
    }

    var estimateDate: LocalDate
        get() = prefsUtil[PREF_ESTIMATE_DATE, ""].let {
            if (!it.isNullOrEmpty()) LocalDate.parse(it) else LocalDate.now()
        }
        set(date) = prefsUtil.put(PREF_ESTIMATE_DATE, date.toString())

    var balanceReading: BalanceReading?
        get() = prefsUtil[PREF_READING, BalanceReading.CREATOR]
        set(reading) = if (reading != null) {
            prefsUtil.put(PREF_READING, reading)
        } else {
            prefsUtil.remove(PREF_READING)
        }

    fun getEstimateDateDataStream(): Observable<LocalDate> {
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

    var lastUpdate: LocalDateTime?
        get() = prefsUtil[PREF_LAST_UPDATE, ""].let {
            if (!it.isNullOrEmpty()) LocalDateTime.parse(it) else null
        }
        set(date) = prefsUtil.put(PREF_LAST_UPDATE, date.toString())

    fun clear() {
        prefsUtil.clear()
    }

    companion object {
        private const val PREF_ESTIMATE_DATE = "PREF_ESTIMATE_DATE"
        private const val PREF_READING = "PREF_READING"
        private const val PREF_LAST_UPDATE = "PREF_LAST_UPDATE"
    }
}
