package com.gb.canibuyit.fcm

import android.util.Log
import com.gb.canibuyit.ACCOUNT_ID_PREPAID
import com.gb.canibuyit.ACCOUNT_ID_RETAIL
import com.gb.canibuyit.TRANSACTION_HISTORY_LENGTH_MONTHS
import com.gb.canibuyit.di.Injector
import com.gb.canibuyit.fcm.model.FcmMonzoData
import com.gb.canibuyit.interactor.MonzoInteractor
import com.gb.canibuyit.notification.LocalNotificationManager
import com.gb.canibuyit.repository.MonzoMapper
import com.gb.canibuyit.util.formatEventTime
import com.gb.canibuyit.util.midnightOfToday
import com.gb.canibuyit.util.millisUntil
import com.gb.canibuyit.util.parseEventDateTime
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject

class MonzoDispatchMessagingService : FirebaseMessagingService() {

    @field:[Inject] lateinit var monzoInteractor: MonzoInteractor
    @field:[Inject] lateinit var monzoMapper: MonzoMapper
    @field:[Inject] lateinit var localNotificationManager: LocalNotificationManager

    init {
        Injector.INSTANCE.graph.inject(this)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: " + remoteMessage.from)
        remoteMessage.notification?.let { Log.d(TAG, "Notification: ${it.title} ${it.body}") }
        remoteMessage.data?.let { it: MutableMap<String, String> ->
            Log.d(TAG, "Data: $it")
            it["monzo_data"]?.let(this@MonzoDispatchMessagingService::handleMonzoPush)
            it["event"]?.let(this@MonzoDispatchMessagingService::handleCalendarEventPush)
        }
    }

    private fun handleMonzoPush(payload: String) {
        val category = Gson().fromJson(payload, FcmMonzoData::class.java)?.data?.let {
            monzoMapper.mapToTransaction(it).category
        }
        category?.let { localNotificationManager.showSpendingInNotification(it.toString()) }
        monzoInteractor.loadSpendings(listOf(ACCOUNT_ID_PREPAID, ACCOUNT_ID_RETAIL), TRANSACTION_HISTORY_LENGTH_MONTHS)
    }

    private fun handleCalendarEventPush(payload: String) {
        val event = Gson().fromJson(payload, Event::class.java)
        val eventStartTime = parseEventDateTime(event.start)
        val eventEndTime = parseEventDateTime(event.end)

        if (isEarlyEvent(eventStartTime, eventEndTime)) {
            val alarmTime = LocalDateTime.of(eventStartTime.toLocalDate(), LocalTime.MIDNIGHT)
                    .minusHours(3)
            localNotificationManager.scheduleEventNotification(alarmTime.millisUntil(), "Event: " + event.title, eventStartTime.formatEventTime(), event.url)
//            localNotificationManager.showSimpleNotification("Event notification scheduled: " + event.title, eventStartTime.formatEventTimePrefix())
        }
    }

    private fun isEarlyEvent(startTime: LocalDateTime, endTime: LocalDateTime): Boolean {
        return !isAllDayEvent(startTime, endTime) && startTime.isAfter(midnightOfToday()) && startTime.hour < 10
    }

    private fun isAllDayEvent(startTime: LocalDateTime, endTime: LocalDateTime): Boolean {
        return startTime.hour == 0 && endTime.hour == 0
    }

    data class Event(val title: String,
                     val start: String,
                     val end: String,
                     val description: String,
                     val where: String,
                     val url: String)
}

private const val TAG = "MonzoDispatchMessagingService"
