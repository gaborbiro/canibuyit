package com.gb.canibuythat.presenter

import com.gb.canibuythat.interactor.SpendingInteractor
import com.gb.canibuythat.screen.SpendingListScreen
import javax.inject.Inject
import javax.inject.Singleton

class SpendingListPresenter @Inject
constructor(private val spendingInteractor: SpendingInteractor) : BasePresenter<SpendingListScreen>() {

    init {
        spendingInteractor.getSpendingsDataStream().subscribe({
            if (!it.loading) {
                if (it.hasError()) {
                    this.onError(it.error!!)
                } else {
                    screen.setData(it.content!!)
                }
            }
        }, this::onError)
    }

    fun fetchSpendings() {
        disposeOnFinish(spendingInteractor.loadSpendings())
    }
}
