package com.gb.canibuythat.fcm

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.gb.canibuythat.MonzoConstants
import com.gb.canibuythat.di.Injector
import com.gb.canibuythat.exception.ErrorHandler
import com.gb.canibuythat.interactor.MonzoDispatchInteractor
import com.gb.canibuythat.interactor.MonzoInteractor
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.iid.FirebaseInstanceIdService
import javax.inject.Inject

class MonzoDispatchInstanceIdService : FirebaseInstanceIdService() {

    @field:[Inject] lateinit var monzoDispatchInteractor: MonzoDispatchInteractor
    @field:[Inject] lateinit var monzoInteractor: MonzoInteractor
    @field:[Inject] lateinit var errorHandler: ErrorHandler
    @field:[Inject] lateinit var appContext: Context

    init {
        Injector.INSTANCE.graph.inject(this)
    }

    override fun onTokenRefresh() {
        val refreshedToken = FirebaseInstanceId.getInstance().token
        Log.d(TAG, "Refreshed token: " + refreshedToken!!)

        sendRegistrationToServer(refreshedToken)
    }

    /**
     * Persist token to third-party servers.

     * @param token The new token.
     */
    private fun sendRegistrationToServer(token: String) {
        monzoDispatchInteractor.register(token)
                .subscribe({ dispatchRegistration ->
                    monzoInteractor.getWebhooks(MonzoConstants.ACCOUNT_ID).subscribe({
                        it.webhooks.forEach { monzoInteractor.deleteWebhook(it).subscribe() }
                        monzoInteractor.registerWebhook(MonzoConstants.ACCOUNT_ID, MonzoConstants.MONZO_DISPATCH_API_WEBHOOK + "/" + dispatchRegistration.hash).subscribe({
                            Toast.makeText(appContext, "Successfully registered for Monzo push notifications " + MonzoConstants.MONZO_DISPATCH_API_WEBHOOK + "/" + dispatchRegistration.hash, Toast.LENGTH_SHORT).show()
                        }, {
                            errorHandler.onErrorSoft(it)
                        })
                    }, {
                        errorHandler.onErrorSoft(it)
                    })
                }, {
                    errorHandler.onErrorSoft(it)
                })
    }

    companion object {
        private val TAG = "MonzoDispatch"
    }
}
