package com.gb.canibuythat.di;

import com.gb.canibuythat.App;
import com.gb.canibuythat.fcm.MonzoDispatchInstanceIdService;
import com.gb.canibuythat.fcm.MonzoDispatchMessagingService;
import com.gb.canibuythat.provider.SpendingProvider;
import com.gb.canibuythat.ui.BalanceReadingInputDialog;
import com.gb.canibuythat.ui.ChartActivity;
import com.gb.canibuythat.ui.LoginActivity;
import com.gb.canibuythat.ui.MainActivity;
import com.gb.canibuythat.ui.SpendingEditorFragment;
import com.gb.canibuythat.ui.SpendingListFragment;

import org.jetbrains.annotations.NotNull;

public interface CanIBuyThatGraph {
    void inject(@NotNull App app);

    void inject(@NotNull MainActivity mainActivity);

    void inject(@NotNull ChartActivity chartActivity);

    void inject(@NotNull SpendingEditorFragment spendingEditorFragment);

    void inject(@NotNull BalanceReadingInputDialog balanceReadingInputDialog);

    void inject(@NotNull SpendingListFragment spendingListFragment);

    void inject(@NotNull SpendingProvider spendingProvider);

    void inject(@NotNull LoginActivity loginActivity);

    void inject(@NotNull MonzoDispatchInstanceIdService monzoDispatchInstanceIdService);

    void inject(@NotNull MonzoDispatchMessagingService monzoDispatchMessagingService);
}
