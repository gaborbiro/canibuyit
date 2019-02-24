package com.gb.canibuyit.fcm

import android.annotation.SuppressLint
import android.util.Log
import com.gb.canibuyit.ACCOUNT_ID_PREPAID
import com.gb.canibuyit.ACCOUNT_ID_RETAIL
import com.gb.canibuyit.TRANSACTION_HISTORY_LENGTH_MONTHS
import com.gb.canibuyit.di.Injector
import com.gb.canibuyit.fcm.model.FcmMonzoData
import com.gb.canibuyit.interactor.MonzoInteractor
import com.gb.canibuyit.interactor.SpendingInteractor
import com.gb.canibuyit.repository.MonzoMapper
import com.gb.canibuyit.util.Logger
import com.gb.canibuyit.util.formatEventTime
import com.gb.canibuyit.util.formatEventTimePrefix
import com.gb.canibuyit.util.midnightOfToday
import com.gb.canibuyit.util.millisUntil
import com.gb.canibuyit.util.parseEventDateTime
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject
import kotlin.math.absoluteValue

class PushMessagingFirebaseService : FirebaseMessagingService() {

    @Inject lateinit var monzoInteractor: MonzoInteractor
    @Inject lateinit var spendingInteractor: SpendingInteractor
    @Inject lateinit var monzoMapper: MonzoMapper
    @Inject lateinit var localNotificationManager: LocalNotificationManager

    init {
        Injector.INSTANCE.graph.inject(this)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: " + remoteMessage.from)
        remoteMessage.data?.let {
            Log.d(TAG, "Data: $it")
            it["monzo_data"]?.let(this@PushMessagingFirebaseService::handleMonzoPush)
            it["event"]?.let(this@PushMessagingFirebaseService::handleCalendarEventPush)
        }
    }

    private fun handleMonzoPush(payload: String) {
        try {
            val category = Gson().fromJson(payload, FcmMonzoData::class.java)?.data?.let {
                monzoMapper.mapToTransaction(it).category
            }
            category?.let {
                doOnNextSpendingUIModel { showSpendingNotification(category.toString()) }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Error handling monzo push", t)
        }
        monzoInteractor.loadSpendings(listOf(ACCOUNT_ID_PREPAID, ACCOUNT_ID_RETAIL), TRANSACTION_HISTORY_LENGTH_MONTHS)
    }

    @SuppressLint("CheckResult")
    private fun doOnNextSpendingUIModel(task: () -> Unit) {
        spendingInteractor.spendingUIModel()
                .filter { !it.loading }
                .map { it.content != null }
                .firstOrError()
                .subscribe({ success ->
                    if (success) {
                        task.invoke()
                    }
                }, {
                    Logger.e(TAG, "SpendingUIModel Error", it)
                })
    }

    @SuppressLint("CheckResult")
    private fun showSpendingNotification(category: String) {
        spendingInteractor.getByMonzoCategory(category)
                .subscribe({ spending ->
                    spending.target?.let { target: Int ->
                        val spent = spending.spent.abs()
                        val absTarget = target.absoluteValue
                        val progress: Float = spent.divide(absTarget.toBigDecimal())
                                .multiply(100.toBigDecimal()).toFloat()
                        localNotificationManager.showSimpleNotification(spending.name, ("%.0f%% (%.0f/%d)").format(progress, spent, absTarget))
                    } ?: let {
                        //                        showSimpleNotification(spending.name, "£%.0f".format(spent))
                    }
                }, {})
    }

    private fun handleCalendarEventPush(payload: String) {
        try {
            val event = Gson().fromJson(payload, CalendarEvent::class.java)
            val eventStartTime = parseEventDateTime(event.start)
            val eventEndTime = parseEventDateTime(event.end)

            if (isEarlyEvent(eventStartTime, eventEndTime)) {
                val alarmTime = LocalDateTime.of(eventStartTime.toLocalDate(), LocalTime.MIDNIGHT)
                        .minusHours(3)
                localNotificationManager.scheduleEventNotification(alarmTime.millisUntil(), "Event: " + event.title, eventStartTime.formatEventTime(), event.url)
                localNotificationManager.showSimpleNotification("Early event notif sheduled: ${event.title}", alarmTime.formatEventTimePrefix())
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Error handling calendar push", t)
        }
    }

    private fun isEarlyEvent(startTime: LocalDateTime, endTime: LocalDateTime): Boolean {
        return !isAllDayEvent(startTime, endTime) && startTime.isAfter(midnightOfToday()) && startTime.hour < 10
    }

    private fun isAllDayEvent(startTime: LocalDateTime, endTime: LocalDateTime): Boolean {
        return startTime.hour == 0 && endTime.hour == 0
    }

    data class CalendarEvent(val title: String,
                             val start: String,
                             val end: String,
                             val description: String,
                             val where: String,
                             val url: String)
}

private const val TAG = "PushMessagingFirebaseService"

