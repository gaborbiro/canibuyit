package com.gb.canibuythat.di;

import com.gb.canibuythat.App;
import com.gb.canibuythat.provider.BudgetProvider;
import com.gb.canibuythat.ui.BalanceReadingInputDialog;
import com.gb.canibuythat.ui.BudgetEditorFragment;
import com.gb.canibuythat.ui.BudgetListFragment;
import com.gb.canibuythat.ui.ChartActivity;
import com.gb.canibuythat.ui.MainActivity;

public interface CanIBuyThatGraph {
    void inject(App app);

    void inject(MainActivity mainActivity);

    void inject(ChartActivity chartActivity);

    void inject(BudgetEditorFragment budgetEditorFragment);

    void inject(BalanceReadingInputDialog balanceReadingInputDialog);

    void inject(BudgetListFragment budgetListFragment);

    void inject(BudgetProvider budgetProvider);
}
