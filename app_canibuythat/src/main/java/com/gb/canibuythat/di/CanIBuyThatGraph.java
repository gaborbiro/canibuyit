package com.gb.canibuythat.di;

import com.gb.canibuythat.App;
import com.gb.canibuythat.provider.SpendingProvider;
import com.gb.canibuythat.ui.BalanceReadingInputDialog;
import com.gb.canibuythat.ui.SpendingEditorFragment;
import com.gb.canibuythat.ui.SpendingListFragment;
import com.gb.canibuythat.ui.ChartActivity;
import com.gb.canibuythat.ui.LoginActivity;
import com.gb.canibuythat.ui.MainActivity;

public interface CanIBuyThatGraph {
    void inject(App app);

    void inject(MainActivity mainActivity);

    void inject(ChartActivity chartActivity);

    void inject(SpendingEditorFragment spendingEditorFragment);

    void inject(BalanceReadingInputDialog balanceReadingInputDialog);

    void inject(SpendingListFragment spendingListFragment);

    void inject(SpendingProvider spendingProvider);

    void inject(LoginActivity loginActivity);
}
