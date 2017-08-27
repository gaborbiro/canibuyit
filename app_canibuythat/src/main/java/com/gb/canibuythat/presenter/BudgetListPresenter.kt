package com.gb.canibuythat.presenter

import com.gb.canibuythat.interactor.BudgetInteractor
import com.gb.canibuythat.screen.BudgetListScreen
import javax.inject.Inject

class BudgetListPresenter @Inject
constructor(private val budgetInteractor: BudgetInteractor) : BasePresenter<BudgetListScreen>() {

    fun fetch() {
        budgetInteractor.all.subscribe({
            screen.setData(it)
        }, {
            this.onError(it)
        })
    }
}
