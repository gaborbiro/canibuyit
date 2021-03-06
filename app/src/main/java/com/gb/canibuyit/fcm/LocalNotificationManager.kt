package com.gb.canibuyit.fcm

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.SystemClock
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.gb.canibuyit.R
import com.gb.canibuyit.feature.spending.view.MainActivity
import javax.inject.Inject

class LocalNotificationManager @Inject constructor(private val applicationContext: Context) {

    private val notificationManager: NotificationManager = applicationContext.getSystemService()!!
    private val notificationColor: Int = ContextCompat.getColor(applicationContext, R.color.primary)

    init {
        createChannels()
    }

    fun showEventNotification(title: String, messageBody: String, url: String) {
        notificationManager.notify("Event", 1, buildNotification(
            pendingIntent = getUrlIntent(url),
            channel = CHANNEL_EVENTS,
            title = title,
            message = messageBody)
        )
    }

    /**
     * Create and show a simple notification containing the received FCM message.

     * @param messageBody FCM message body received.
     */
    fun showSimpleNotification(title: String?, messageBody: String?) {
        notificationManager.notify("Monzo", 0, buildNotification(pendingIntent = getLaunchIntent(),
            channel = CHANNEL_SPENDINGS,
            title = title,
            message = messageBody)
        )
    }

    /**
     * @param delay after how much time(in millis) from current time you want to schedule the notification
     */
    fun scheduleEventNotification(delay: Long, title: String, messageBody: String, url: String) {
        val notification = buildNotification(
            pendingIntent = getUrlIntent(url),
            channel = CHANNEL_EVENTS,
            title = title,
            message = messageBody)

        val notificationIntent =
            Intent(applicationContext, MyEventAlarmReceiver::class.java).apply {
                putExtra(MyEventAlarmReceiver.NOTIFICATION, notification)
            }
        val pendingIntent = PendingIntent.getBroadcast(applicationContext, 0, notificationIntent,
            PendingIntent.FLAG_CANCEL_CURRENT)

        val futureInMillis = SystemClock.elapsedRealtime() + delay
        val alarmManager: AlarmManager = applicationContext.getSystemService()!!
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, futureInMillis, pendingIntent)
    }

    private fun getUrlIntent(url: String): PendingIntent {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(url)
        }
        return PendingIntent.getActivity(applicationContext, 0, intent, 0)
    }

    private fun getLaunchIntent(): PendingIntent {
        return PendingIntent.getActivity(applicationContext, 0, getMainScreenIntent(), 0)
    }

    private fun getMainScreenIntent(): Intent {
        return Intent(applicationContext, MainActivity::class.java)
    }

    private fun buildNotification(pendingIntent: PendingIntent,
                                  channel: String,
                                  title: String?,
                                  message: String?,
                                  priority: Int = NotificationCompat.PRIORITY_DEFAULT,
                                  @ColorInt color: Int = notificationColor,
                                  @DrawableRes smallIcon: Int = R.drawable.piggybank,
                                  alertOnlyOnce: Boolean = false,
                                  autoCancel: Boolean = true,
                                  sound: Uri? = RingtoneManager.getDefaultUri(
                                      RingtoneManager.TYPE_NOTIFICATION),
                                  vibrate: LongArray? = null): Notification {
        return NotificationCompat.Builder(applicationContext, channel)
            .setSmallIcon(smallIcon)
            .setColor(color)
            .setContentTitle(title)
            .setContentText(message)
            .setContentIntent(pendingIntent)
            .setPriority(priority)
            .setOnlyAlertOnce(alertOnlyOnce)
            .setAutoCancel(autoCancel)
            .setSound(sound)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setVibrate(vibrate)
            .build()
    }

    private fun createChannels() {
        val spendingsChannel = NotificationChannel(CHANNEL_SPENDINGS, "Spending alerts",
            NotificationManager.IMPORTANCE_LOW)
        spendingsChannel.setShowBadge(false)
        notificationManager.createNotificationChannel(spendingsChannel)
        val calendarEventsChannel = NotificationChannel(CHANNEL_EVENTS, "Calendar event alerts",
            NotificationManager.IMPORTANCE_DEFAULT)
        notificationManager.createNotificationChannel(calendarEventsChannel)
    }
}

private const val CHANNEL_SPENDINGS = "spendings"
private const val CHANNEL_EVENTS = "events"