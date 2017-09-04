package com.gb.canibuythat.presenter

import com.gb.canibuythat.interactor.SpendingInteractor
import com.gb.canibuythat.screen.SpendingListScreen
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpendingListPresenter @Inject
constructor(private val spendingInteractor: SpendingInteractor) : BasePresenter<SpendingListScreen>() {

    init {
        spendingInteractor.subject.subscribe({
            screen.setData(it)
        }, this::onError)
    }

    fun fetchSpendings() {
        spendingInteractor.loadSpendings()
    }
}
