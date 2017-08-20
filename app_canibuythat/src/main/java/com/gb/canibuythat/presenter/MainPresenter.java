package com.gb.canibuythat.presenter;

import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;

import com.gb.canibuythat.CredentialsProvider;
import com.gb.canibuythat.MonzoConstants;
import com.gb.canibuythat.di.Injector;
import com.gb.canibuythat.interactor.BudgetInteractor;
import com.gb.canibuythat.interactor.MonzoInteractor;
import com.gb.canibuythat.screen.MainScreen;
import com.gb.canibuythat.ui.FileDialogActivity;

import java.util.List;

import javax.inject.Inject;

public class MainPresenter extends BasePresenter {

    @Inject MonzoInteractor monzoInteractor;
    @Inject BudgetInteractor budgetInteractor;
    @Inject CredentialsProvider credentialsProvider;

    private MainScreen screen;

    public MainPresenter(MainScreen screen) {
        this.screen = screen;
    }

    @Override
    protected void inject() {
        Injector.INSTANCE.getGraph().inject(this);
    }

    public void fetchBalance() {
        budgetInteractor.calculateBalance().subscribe(screen::setBalanceInfo, this::onError);
    }

    public void handleDeepLink(Intent intent) {
        Uri data = intent.getData();
        if (data.getAuthority().equals(MonzoConstants.MONZO_AUTH_AUTHORITY)) {
            List<String> pathSegments = data.getPathSegments();

            if (pathSegments.get(0).equals(MonzoConstants.MONZO_AUTH_PATH_BASE)) {
                if (pathSegments.get(1).equals(MonzoConstants.MONZO_AUTH_PATH_CALLBACK)) {
                    // finished email authentication -> exchange code for auth token
                    String authorizationCode = data.getQueryParameter(MonzoConstants.MONZO_OAUTH_PARAM_AUTHORIZATION_CODE);
                    login(authorizationCode);
                }
            }
        }
    }

    private void login(String authorizationCode) {
        monzoInteractor.login(authorizationCode).subscribe(login -> {
            credentialsProvider.setAccessToken(login.getAccessToken());
            credentialsProvider.setRefreshToken(login.getRefreshToken());
        }, this::onError);
    }

    public void chartButtonClicked() {
        screen.showChartScreen();
    }

    public void doMonzoStuff() {
        if (TextUtils.isEmpty(credentialsProvider.getAccessToken())) {
            screen.showLoginActivity();
        } else {
            monzoInteractor.transactions(MonzoConstants.ACCOUNT_ID)
                    .subscribe(transactions -> {

                    }, this::onError);
        }
    }

    public void exportDatabase() {
        budgetInteractor.exportDatabase().subscribe(() -> {}, this::onError);
    }

    public void onImportDatabase() {
        String directory = Environment.getExternalStorageDirectory().getPath() + "/CanIBuyThat/";
        screen.showFilePickerActivity(directory);
    }

    public void onDatabaseFileSelected(String path) {
        budgetInteractor.importDatabase(path).subscribe(this::fetchBalance, this::onError);
    }

    public void updateBalance() {
        screen.showBalanceUpdateDialog();
    }

    public void showEditorScreenForBudgetItem(int id) {
        screen.showEditorScreen(id);
    }

    public void showEditorScreen() {
        screen.showEditorScreen(null);
    }
}
