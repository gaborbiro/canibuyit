package com.gb.canibuythat.presenter

import com.gb.canibuythat.ACCOUNT_ID_PREPAID
import com.gb.canibuythat.ACCOUNT_ID_RETAIL
import com.gb.canibuythat.MONZO_DISPATCH_API_WEBHOOK
import com.gb.canibuythat.UserPreferences
import com.gb.canibuythat.interactor.MonzoDispatchInteractor
import com.gb.canibuythat.interactor.MonzoInteractor
import com.gb.canibuythat.screen.MonzoDispatchScreen
import javax.inject.Inject

class MonzoDispatchPresenter @Inject
constructor(val monzoInteractor: MonzoInteractor,
            val monzoDispatchInteractor: MonzoDispatchInteractor,
            val userPreferences: UserPreferences) : BasePresenter<MonzoDispatchScreen>() {

    fun sendFCMTokenToServer(token: String) {
        monzoDispatchInteractor.register(token)
                .subscribe({ dispatchRegistration ->
                    monzoInteractor.getWebhooks(ACCOUNT_ID_PREPAID).subscribe({
                        it.webhooks.forEach { monzoInteractor.deleteWebhook(it).subscribe() }
                        monzoInteractor.registerWebhook(ACCOUNT_ID_PREPAID, MONZO_DISPATCH_API_WEBHOOK + "/" + dispatchRegistration.hash).subscribe({
                            getScreen().showToast("Successfully registered for prepaid Monzo push notifications " + MONZO_DISPATCH_API_WEBHOOK + "/" + dispatchRegistration.hash)
                        }, {
                            errorHandler.onErrorSoft(it)
                        })
                    }, {
                        errorHandler.onErrorSoft(it)
                    })
                    monzoInteractor.getWebhooks(ACCOUNT_ID_RETAIL).subscribe({
                        it.webhooks.forEach { monzoInteractor.deleteWebhook(it).subscribe() }
                        monzoInteractor.registerWebhook(ACCOUNT_ID_RETAIL, MONZO_DISPATCH_API_WEBHOOK + "/" + dispatchRegistration.hash).subscribe({
                            getScreen().showToast("Successfully registered for retail Monzo push notifications " + MONZO_DISPATCH_API_WEBHOOK + "/" + dispatchRegistration.hash)
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
}