package com.gb.canibuythat.fcm

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.gb.canibuythat.di.Injector
import com.gb.canibuythat.exception.ErrorHandler
import com.gb.canibuythat.interactor.MonzoDispatchInteractor
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.iid.FirebaseInstanceIdService
import javax.inject.Inject

class MonzoDispatchInstanceIdService : FirebaseInstanceIdService() {

    @field:[Inject] lateinit var monzoDispatchInteractor: MonzoDispatchInteractor
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
        monzoDispatchInteractor.register("CanIBuyThat", token)
                .subscribe({
                    Toast.makeText(appContext, "Successfully register for Monzo push notifications", Toast.LENGTH_SHORT).show()
                }, {
                    errorHandler.onErrorSoft(it)
                })
    }

    companion object {
        private val TAG = "MonzoDispatch"
    }
}
