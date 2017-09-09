package com.gb.canibuythat.presenter

import android.content.Intent
import android.os.Environment
import com.gb.canibuythat.CredentialsProvider
import com.gb.canibuythat.MonzoConstants
import com.gb.canibuythat.interactor.BackupingInteractor
import com.gb.canibuythat.interactor.MonzoInteractor
import com.gb.canibuythat.interactor.SpendingInteractor
import com.gb.canibuythat.screen.MainScreen
import javax.inject.Inject

class MainPresenter @Inject
constructor(val monzoInteractor: MonzoInteractor,
            val spendingInteractor: SpendingInteractor,
            val backupingInteractor: BackupingInteractor,
            val credentialsProvider: CredentialsProvider) : BasePresenter<MainScreen>() {

    init {
        monzoInteractor.getSpendingsDataStream().subscribe({
            if (it.loading) screen.showProgress() else screen.hideProgress()
        }, this::onError)
        monzoInteractor.getLoginDataStream().subscribe({
            if (it.loading) {
                screen.showProgress()
            } else {
                if (it.hasError()) {
                    this.onError(it.error!!)
                } else {
                    screen.hideProgress()
                    credentialsProvider.accessToken = it.content!!.accessToken
                    credentialsProvider.refreshToken = it.content!!.refreshToken
                    screen.showToast("You are now logged in")
                }
            }
        }, this::onError)
    }

    fun fetchBalance() {
        disposeOnFinish(spendingInteractor.calculateBalance()
                .doOnSubscribe { screen.showProgress() }
                .doAfterTerminate { screen.hideProgress() }
                .subscribe(screen::setBalanceInfo, this::onError))
    }

    fun calculateCategoryBalance() {
        spendingInteractor.calculateCategoryBalance()
    }

    fun handleDeepLink(intent: Intent) {
        val data = intent.data
        if (data != null && data.authority == MonzoConstants.MONZO_AUTH_AUTHORITY) {
            val pathSegments = data.pathSegments

            if (pathSegments[0] == MonzoConstants.MONZO_AUTH_PATH_BASE) {
                if (pathSegments[1] == MonzoConstants.MONZO_AUTH_PATH_CALLBACK) {
                    // finished email authentication -> exchange code for auth token
                    val authorizationCode = data.getQueryParameter(MonzoConstants.MONZO_OAUTH_PARAM_AUTHORIZATION_CODE)
                    login(authorizationCode)
                }
            }
        }
    }

    private fun login(authorizationCode: String) {
        disposeOnFinish(monzoInteractor.login(authorizationCode))
    }

    fun chartButtonClicked() {
        screen.showChartScreen()
    }

    fun fetchMonzoData() {
        if (!credentialsProvider.isSession()) {
            screen.showLoginActivity()
        } else {
            disposeOnFinish(monzoInteractor.loadSpendings(MonzoConstants.ACCOUNT_ID))
        }
    }

    fun deleteAllSpendings() {
        spendingInteractor.clearSpendings()
    }

    fun exportDatabase() {
        backupingInteractor.exportSpendings().subscribe({screen.showToast("Database exported")}, this::onError)
    }

    fun onImportDatabase(importType: MainScreen.SpendingsImportType) {
        val directory = Environment.getExternalStorageDirectory().path + "/CanIBuyThat/"
        screen.showFilePickerActivity(directory, importType)
    }

    fun onImportSpendings(path: String, importType: MainScreen.SpendingsImportType) {
        when(importType) {
            MainScreen.SpendingsImportType.ALL -> backupingInteractor.importAllSpendings(path).subscribe(this::fetchBalance, this::onError)
            MainScreen.SpendingsImportType.MONZO -> backupingInteractor.importMonzoSpendings(path).subscribe(this::fetchBalance, this::onError)
            MainScreen.SpendingsImportType.NON_MONZO -> backupingInteractor.importNonMonzoSpendings(path).subscribe(this::fetchBalance, this::onError)
        }
    }

    fun updateBalance() {
        screen.showBalanceUpdateDialog()
    }

    fun showEditorScreenForSpending(id: Int) {
        screen.showEditorScreen(id)
    }

    fun showEditorScreen() {
        screen.showEditorScreen(null)
    }
}
