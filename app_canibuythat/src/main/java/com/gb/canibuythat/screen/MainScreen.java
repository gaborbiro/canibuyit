package com.gb.canibuythat.screen;

import com.gb.canibuythat.model.Balance;

public interface MainScreen extends Screen {
    void showChartScreen();

    void showLoginActivity();

    void setBalanceInfo(Balance balance);

    void showFilePickerActivity(String directory);

    void showBalanceUpdateDialog();

    void showEditorScreen(final Integer budgetItemId);
}
