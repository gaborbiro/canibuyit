package com.gb.canibuyit.fcm

import android.app.Notification
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class MyEventAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val NOTIFICATION = "notification"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification: Notification = intent.getParcelableExtra(NOTIFICATION)
        notificationManager.notify(0, notification)
    }
}