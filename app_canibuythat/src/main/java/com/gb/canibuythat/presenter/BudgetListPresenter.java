package com.gb.canibuythat.presenter;

import com.gb.canibuythat.interactor.BudgetInteractor;
import com.gb.canibuythat.screen.BudgetListScreen;

import javax.inject.Inject;

public class BudgetListPresenter extends BasePresenter<BudgetListScreen> {

    private BudgetInteractor budgetInteractor;

    @Inject
    public BudgetListPresenter(BudgetInteractor budgetInteractor) {
        this.budgetInteractor = budgetInteractor;
    }

    public void fetch() {
        budgetInteractor.getAll().subscribe(budgetItems -> {
            getScreen().setData(budgetItems);
        }, this::onError);
    }
}
