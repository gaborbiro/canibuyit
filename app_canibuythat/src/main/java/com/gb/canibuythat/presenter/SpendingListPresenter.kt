package com.gb.canibuythat.presenter

import com.gb.canibuythat.interactor.SpendingInteractor
import com.gb.canibuythat.screen.SpendingListScreen
import javax.inject.Inject

class SpendingListPresenter @Inject
constructor(private val spendingInteractor: SpendingInteractor) : BasePresenter<SpendingListScreen>() {

    init {
        disposeOnFinish(spendingInteractor.getSpendingsDataStream().subscribe({
            if (!it.loading) {
                it.error?.let(this::onError)
                it.content
                        ?.sortedBy { -Math.abs(it.valuePerMonth) }
                        ?.sortedBy { !it.enabled }
                        ?.let(getScreen()::setData)
            }
        }, this::onError))
    }

    fun fetchSpendings() {
        spendingInteractor.loadSpendings()
    }
}
