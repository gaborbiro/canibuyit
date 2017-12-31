package com.gb.canibuythat.fcm

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.gb.canibuythat.CredentialsProvider
import com.gb.canibuythat.di.Injector
import com.gb.canibuythat.exception.ErrorHandler
import com.gb.canibuythat.presenter.MonzoDispatchPresenter
import com.gb.canibuythat.screen.MonzoDispatchScreen
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.iid.FirebaseInstanceIdService
import javax.inject.Inject

class MonzoDispatchInstanceIdService : FirebaseInstanceIdService(), MonzoDispatchScreen {

    @field:[Inject] lateinit var errorHandler: ErrorHandler
    @field:[Inject] lateinit var appContext: Context
    @field:[Inject] lateinit var presenter: MonzoDispatchPresenter
    @field:[Inject] lateinit var credentialsProvider: CredentialsProvider

    init {
        Injector.INSTANCE.graph.inject(this)
        presenter.setScreen(this)
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

    companion object {
        private val TAG = "MonzoDispatch"
    }
}
