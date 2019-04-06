package com.gb.canibuyit.feature.spending.view

import com.gb.canibuyit.base.view.BasePresenter
import com.gb.canibuyit.feature.spending.data.SpendingInteractor
import javax.inject.Inject

class SpendingListPresenter @Inject
constructor(private val spendingInteractor: SpendingInteractor) :
        BasePresenter<SpendingListScreen>() {

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
