package com.gb.canibuythat.fcm

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.support.v4.app.NotificationCompat
import android.util.Log
import com.gb.canibuythat.ACCOUNT_ID_PREPAID
import com.gb.canibuythat.ACCOUNT_ID_RETAIL
import com.gb.canibuythat.R
import com.gb.canibuythat.di.Injector
import com.gb.canibuythat.fcm.model.FcmMonzoData
import com.gb.canibuythat.interactor.MonzoInteractor
import com.gb.canibuythat.interactor.SpendingInteractor
import com.gb.canibuythat.repository.MonzoMapper
import com.gb.canibuythat.ui.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import io.reactivex.disposables.Disposable
import javax.inject.Inject

class MonzoDispatchMessagingService : FirebaseMessagingService() {

    @field:[Inject] lateinit var monzoInteractor: MonzoInteractor
    @field:[Inject] lateinit var spendingInteractor: SpendingInteractor
    @field:[Inject] lateinit var monzoMapper: MonzoMapper

    private var disposable: Disposable? = null

    init {
        Injector.INSTANCE.graph.inject(this)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage?) {
        Log.d(TAG, "From: " + remoteMessage!!.from)
        remoteMessage.notification?.let { Log.d(TAG, "Message notification payload: " + it.body) }
        remoteMessage.data?.let {
            Log.d(TAG, "Message data payload: " + it)
            if (it.containsKey("monzo_data")) {
                val category =  Gson().fromJson(it["monzo_data"], FcmMonzoData::class.java)?.data?.let {
                    monzoMapper.mapToTransaction(it).category
                }
                category?.let { showSpendingInNotification(it.toString()) }
            }
            if (it.containsKey("notification")) {
                val notification = Gson().fromJson(it["notification"], Notification::class.java)
                sendNotification(notification.title, notification.body)
                showSpendingInNotification(notification.body)
            }
            monzoInteractor.loadSpendings(listOf(ACCOUNT_ID_PREPAID, ACCOUNT_ID_RETAIL))
        }
    }

    private fun showSpendingInNotification(category: String) {
        disposable?.dispose()
        disposable = spendingInteractor.getSpendingsDataStream().subscribe({
            if (!it.loading && !it.hasError()) {
                spendingInteractor.getByMonzoCategory(category).subscribe({ spending ->
                    val spent = Math.abs(spending.spent ?: 0.0)
                    spending.target?.let {
                        val progress: Float = (spent / it).toFloat() * 100
                        sendNotification(spending.name!!, ("%.0f%%(%.0f/%.0f)").format(progress, spent, it))
                    } ?: let {
//                        sendNotification(spending.name!!, "£%.0f".format(spent))
                    }
                }, {})
                disposable?.dispose()
            }
        }, {
            disposable?.dispose()
        })
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

    data class Notification(val title: String, val body: String)
}
