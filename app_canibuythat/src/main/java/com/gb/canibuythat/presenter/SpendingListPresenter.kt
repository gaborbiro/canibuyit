package com.gb.canibuythat.presenter

import com.gb.canibuythat.interactor.SpendingInteractor
import com.gb.canibuythat.screen.SpendingListScreen
import javax.inject.Inject

class SpendingListPresenter @Inject
constructor(private val spendingInteractor: SpendingInteractor) : BasePresenter<SpendingListScreen>() {

    init {
        disposeOnFinish(spendingInteractor.getSpendingsDataStream().subscribe({
            if (!it.loading) {
                if (it.hasError()) {
                    this.onError(it.error!!)
                } else {
                    getScreen().setData(it.content!!.sortedBy { it.value }.sortedBy { !it.enabled })
                }
            }
        }, this::onError))
    }

    fun fetchSpendings() {
        spendingInteractor.loadSpendings()
    }
}
