package com.gb.canibuyit.feature.spending.view

import com.gb.canibuyit.base.view.BasePresenter
import com.gb.canibuyit.feature.spending.data.SpendingInteractor
import javax.inject.Inject

class SpendingListPresenter @Inject
constructor(private val spendingInteractor: SpendingInteractor) : BasePresenter() {

    val screen: SpendingListScreen by screenDelegate()

    init {
        disposeOnDestroy(spendingInteractor.spendingSubject.subscribe({ lce ->
            if (!lce.loading) {
                lce.error?.let(this::onError)
                lce.content
                        ?.sortedBy { -it.valuePerMonth.abs() }
                        ?.sortedBy { !it.enabled }
                        ?.let(screen::setData)
            }
        }, this::onError))
    }

    fun fetchSpendings() {
        spendingInteractor.loadSpendings()
    }
}
