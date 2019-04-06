package com.gb.canibuyit.fcm

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LifecycleObserver
import com.gb.canibuyit.di.Injector
import com.gb.canibuyit.feature.dispatch.view.DispatchPresenter
import com.gb.canibuyit.feature.dispatch.view.DispatchScreen
import com.gb.canibuyit.feature.monzo.CredentialsProvider
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.iid.FirebaseInstanceIdService
import javax.inject.Inject

class FirebaseInstanceIdService : FirebaseInstanceIdService(), DispatchScreen {

    @Inject lateinit var appContext: Context
    @Inject lateinit var presenter: DispatchPresenter
    @Inject lateinit var credentialsProvider: CredentialsProvider

    init {
        Injector.INSTANCE.graph.inject(this)
        presenter.screenReference = this
    }

    override fun onTokenRefresh() {
        if (!credentialsProvider.accessToken.isNullOrEmpty() || credentialsProvider.isRefresh()) {
            val refreshedToken = FirebaseInstanceId.getInstance().token
            Log.d(TAG, "Refreshed token: " + refreshedToken!!)
            presenter.sendFCMTokenToServer(refreshedToken)
        }
    }

    override fun showProgress() {
    }

    override fun showToast(message: String) {
        Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show()
    }

    override fun hideProgress() {
    }

    override fun addLifecycleObserver(observer: LifecycleObserver) {
        // ignore
    }
}

private const val TAG = "MonzoDispatch"
