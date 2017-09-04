package com.gb.canibuythat.fcm

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.support.v4.app.NotificationCompat
import android.util.Log
import com.gb.canibuythat.MonzoConstants
import com.gb.canibuythat.R
import com.gb.canibuythat.di.Injector
import com.gb.canibuythat.interactor.MonzoInteractor
import com.gb.canibuythat.ui.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import javax.inject.Inject

class MonzoDispatchMessagingService : FirebaseMessagingService() {

    @field:[Inject] lateinit var monzoInteractor: MonzoInteractor

    init {
        Injector.INSTANCE.graph.inject(this)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage?) {
        Log.d(TAG, "From: " + remoteMessage!!.from)
        if (remoteMessage.data.size > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.data)
        }
        if (remoteMessage.notification != null) {
            val notification = remoteMessage.notification
            Log.d(TAG, "Message Notification: " + notification.body + " " + notification.body)
            sendNotification(notification.title!!, notification.body!!)
        }
        monzoInteractor.loadTransactions(MonzoConstants.ACCOUNT_ID)
    }

    /**
     * Create and show a simple notification containing the received FCM message.

     * @param messageBody FCM message body received.
     */
    private fun sendNotification(title: String, messageBody: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT)

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.piggybank)
                .setContentTitle(title)
                .setContentText(messageBody)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify("Monzo", 0, notificationBuilder.build())
    }

    companion object {

        private val TAG = "MonzoDispatch"
    }
}
