package com.gb.canibuythat.fcm

import android.util.Log
import com.gb.canibuythat.ACCOUNT_ID_PREPAID
import com.gb.canibuythat.ACCOUNT_ID_RETAIL
import com.gb.canibuythat.TRANSACTION_HISTORY_LENGTH_MONTHS
import com.gb.canibuythat.di.Injector
import com.gb.canibuythat.fcm.model.FcmMonzoData
import com.gb.canibuythat.interactor.MonzoInteractor
import com.gb.canibuythat.notification.LocalNotificationManager
import com.gb.canibuythat.repository.MonzoMapper
import com.gb.canibuythat.util.*
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
        remoteMessage.data?.let {
            Log.d(TAG, "Data: $it")
            if (it.containsKey("monzo_data")) {
                val category = Gson().fromJson(it["monzo_data"], FcmMonzoData::class.java)?.data?.let {
                    monzoMapper.mapToTransaction(it).category
                }
                category?.let { localNotificationManager.showSpendingInNotification(it.toString()) }
                monzoInteractor.loadSpendings(listOf(ACCOUNT_ID_PREPAID, ACCOUNT_ID_RETAIL), TRANSACTION_HISTORY_LENGTH_MONTHS)
            }
            if (it.containsKey("event")) {
                val event = Gson().fromJson(it["event"], Event::class.java)
                val eventDateTime = parseEventDateTime(event.start)

                if (eventDateTime.isAfter(midnight()) && eventDateTime.hour < 10) {
                    val alarmTime = LocalDateTime.of(eventDateTime.toLocalDate(), LocalTime.MIDNIGHT).minusHours(3)
                    localNotificationManager.scheduleEventNotification(alarmTime.millisUntil(), "Event: " + event.title, eventDateTime.formatEventTime(), event.url)
                    localNotificationManager.showSimpleNotification("Event notification scheduled: " + event.title, eventDateTime.formatEventTimePrefix())
                }
            }
        }
    }

    companion object {
        private val TAG = "MonzoDispatch"
    }

    data class Event(val title: String,
                     val start: String,
                     val description: String,
                     val where: String,
                     val url: String)
}
