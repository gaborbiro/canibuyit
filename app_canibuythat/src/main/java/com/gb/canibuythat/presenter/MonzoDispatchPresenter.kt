package com.gb.canibuythat.presenter

import com.gb.canibuythat.MonzoConstants
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
                    monzoInteractor.getWebhooks(MonzoConstants.ACCOUNT_ID_PREPAID).subscribe({
                        it.webhooks.forEach { monzoInteractor.deleteWebhook(it).subscribe() }
                        monzoInteractor.registerWebhook(MonzoConstants.ACCOUNT_ID_PREPAID, MonzoConstants.MONZO_DISPATCH_API_WEBHOOK + "/" + dispatchRegistration.hash).subscribe({
                            getScreen().showToast("Successfully registered for prepaid Monzo push notifications " + MonzoConstants.MONZO_DISPATCH_API_WEBHOOK + "/" + dispatchRegistration.hash)
                        }, {
                            errorHandler.onErrorSoft(it)
                        })
                    }, {
                        errorHandler.onErrorSoft(it)
                    })
                    monzoInteractor.getWebhooks(MonzoConstants.ACCOUNT_ID_RETAIL).subscribe({
                        it.webhooks.forEach { monzoInteractor.deleteWebhook(it).subscribe() }
                        monzoInteractor.registerWebhook(MonzoConstants.ACCOUNT_ID_RETAIL, MonzoConstants.MONZO_DISPATCH_API_WEBHOOK + "/" + dispatchRegistration.hash).subscribe({
                            getScreen().showToast("Successfully registered for retail Monzo push notifications " + MonzoConstants.MONZO_DISPATCH_API_WEBHOOK + "/" + dispatchRegistration.hash)
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