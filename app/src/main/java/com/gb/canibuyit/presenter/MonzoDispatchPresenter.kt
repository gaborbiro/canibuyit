package com.gb.canibuyit.presenter

import com.gb.canibuyit.ACCOUNT_ID_PREPAID
import com.gb.canibuyit.ACCOUNT_ID_RETAIL
import com.gb.canibuyit.MONZO_DISPATCH_API_WEBHOOK
import com.gb.canibuyit.interactor.MonzoDispatchInteractor
import com.gb.canibuyit.interactor.MonzoInteractor
import com.gb.canibuyit.screen.MonzoDispatchScreen
import javax.inject.Inject

class MonzoDispatchPresenter @Inject
constructor(private val monzoInteractor: MonzoInteractor,
            private val monzoDispatchInteractor: MonzoDispatchInteractor) : BasePresenter<MonzoDispatchScreen>() {

    fun sendFCMTokenToServer(token: String) {
        monzoDispatchInteractor.register(token)
                .subscribe({ dispatchRegistration ->
                    monzoInteractor.getWebhooks(ACCOUNT_ID_PREPAID).subscribe({
                        it.webhooks.forEach { monzoInteractor.deleteWebhook(it).subscribe() }
                        monzoInteractor.registerWebhook(ACCOUNT_ID_PREPAID, MONZO_DISPATCH_API_WEBHOOK + "/" + dispatchRegistration.hash)
                                .subscribe({
                                    getScreen().showToast("Successfully registered for prepaid Monzo push notifications " + MONZO_DISPATCH_API_WEBHOOK + "/" + dispatchRegistration.hash)
                                }, {
                                    errorHandler.onErrorSoft(it)
                                })
                    }, {
                        errorHandler.onErrorSoft(it)
                    })
                    monzoInteractor.getWebhooks(ACCOUNT_ID_RETAIL).subscribe({
                        it.webhooks.forEach { monzoInteractor.deleteWebhook(it).subscribe() }
                        monzoInteractor.registerWebhook(ACCOUNT_ID_RETAIL, MONZO_DISPATCH_API_WEBHOOK + "/" + dispatchRegistration.hash)
                                .subscribe({
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