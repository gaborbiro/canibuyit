package com.gb.canibuyit.feature.dispatch.view

import android.annotation.SuppressLint
import com.gb.canibuyit.base.view.BasePresenter
import com.gb.canibuyit.feature.dispatch.MONZO_DISPATCH_API_WEBHOOK
import com.gb.canibuyit.feature.dispatch.data.DispatchInteractor
import com.gb.canibuyit.feature.dispatch.model.DispatchRegistration
import com.gb.canibuyit.feature.monzo.ACCOUNT_ID_RETAIL
import com.gb.canibuyit.feature.monzo.data.MonzoInteractor
import javax.inject.Inject

class DispatchPresenter @Inject
constructor(private val monzoInteractor: MonzoInteractor,
            private val dispatchInteractor: DispatchInteractor) : BasePresenter<DispatchScreen>() {

    @SuppressLint("CheckResult")
    fun sendFCMTokenToServer(token: String) {
        dispatchInteractor.register(token)
                .subscribe(this::handleRetailWebhooks, errorHandler::onErrorSoft)
    }

    @SuppressLint("CheckResult")
    private fun handleRetailWebhooks(dispatchRegistration: DispatchRegistration) {
        monzoInteractor.getWebhooks(ACCOUNT_ID_RETAIL).subscribe({
            it.webhooks.forEach { monzoInteractor.deleteWebhook(it).subscribe() }
            monzoInteractor.registerWebhook(ACCOUNT_ID_RETAIL,
                    MONZO_DISPATCH_API_WEBHOOK + "/" + dispatchRegistration.hash)
                    .subscribe({
                        getScreen().showToast(
                                "Successfully registered for retail Monzo push notifications $MONZO_DISPATCH_API_WEBHOOK/${dispatchRegistration.hash}")
                    }, errorHandler::onErrorSoft)
        }, errorHandler::onErrorSoft)
    }
}