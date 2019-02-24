package com.gb.canibuyit.presenter

import com.gb.canibuyit.interactor.SpendingInteractor
import com.gb.canibuyit.screen.SpendingListScreen
import javax.inject.Inject

class SpendingListPresenter @Inject
constructor(private val spendingInteractor: SpendingInteractor) : BasePresenter<SpendingListScreen>() {

    init {
        disposeOnFinish(spendingInteractor.spendingModel.subscribe({ lce ->
            if (!lce.loading) {
                lce.error?.let(this::onError)
                lce.content
                        ?.sortedBy { -it.valuePerMonth.abs() }
                        ?.sortedBy { !it.enabled }
                        ?.let(getScreen()::setData)
            }
        }, this::onError))
    }

    fun fetchSpendings() {
        spendingInteractor.loadSpendings()
    }
}
