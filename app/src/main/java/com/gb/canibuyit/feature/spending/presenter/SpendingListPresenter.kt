package com.gb.canibuyit.feature.spending.presenter

import com.gb.canibuyit.feature.spending.data.SpendingInteractor
import com.gb.canibuyit.feature.spending.screen.SpendingListScreen
import com.gb.canibuyit.presenter.BasePresenter
import javax.inject.Inject

class SpendingListPresenter @Inject
constructor(private val spendingInteractor: SpendingInteractor) : BasePresenter<SpendingListScreen>() {

    init {
        disposeOnFinish(spendingInteractor.subscribeToSpendings({ lce ->
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
