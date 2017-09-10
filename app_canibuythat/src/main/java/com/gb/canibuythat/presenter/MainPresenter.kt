package com.gb.canibuythat.presenter

import android.content.Intent
import android.os.Environment
import com.gb.canibuythat.CredentialsProvider
import com.gb.canibuythat.MonzoConstants
import com.gb.canibuythat.UserPreferences
import com.gb.canibuythat.interactor.BackupingInteractor
import com.gb.canibuythat.interactor.MonzoInteractor
import com.gb.canibuythat.interactor.SpendingInteractor
import com.gb.canibuythat.model.Balance
import com.gb.canibuythat.screen.MainScreen
import javax.inject.Inject

class MainPresenter @Inject
constructor(val monzoInteractor: MonzoInteractor,
            val spendingInteractor: SpendingInteractor,
            val backupingInteractor: BackupingInteractor,
            val credentialsProvider: CredentialsProvider,
            val userPreferences: UserPreferences) : BasePresenter<MainScreen>() {

    init {
        disposeOnFinish(monzoInteractor.getSpendingsDataStream().subscribe({
            if (it.loading) getScreen().showProgress() else getScreen().hideProgress()
            if (!it.loading) fetchBalance()
        }, this::onError))
        disposeOnFinish(monzoInteractor.getLoginDataStream().subscribe({
            if (it.loading) {
                getScreen().showProgress()
            } else {
                if (it.hasError()) {
                    this.onError(it.error!!)
                } else {
                    getScreen().hideProgress()
                    credentialsProvider.accessToken = it.content!!.accessToken
                    credentialsProvider.refreshToken = it.content!!.refreshToken
                    getScreen().showToast("You are now logged in")
                }
            }
        }, this::onError))
    }

    override fun onScreenSet() {
        disposeOnFinish(userPreferences.getBalanceReadingDataStream().subscribe({ fetchBalance() }))
        disposeOnFinish(userPreferences.getEstimateDateDataStream().subscribe({
            fetchBalance()
        }))
    }

    private fun fetchBalance() {
        disposeOnFinish(spendingInteractor.getBalance()
                .doOnSubscribe { getScreen().showProgress() }
                .doAfterTerminate { getScreen().hideProgress() }
                .subscribe(getScreen()::setBalanceInfo, {
                    getScreen().setBalanceInfo(Balance())
                    this.onError(com.gb.canibuythat.exception.DomainException("Cannot calculate balance. See logs", it))
                }))
    }

    fun fetchCategoryBalance() {
        getScreen().showCategoryBalance(spendingInteractor.getCategoryBalance())
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
        getScreen().showChartScreen()
    }

    fun fetchMonzoData() {
        if (!credentialsProvider.isSession()) {
            getScreen().showLoginActivity()
        } else {
            disposeOnFinish(monzoInteractor.loadSpendings(MonzoConstants.ACCOUNT_ID))
        }
    }

    fun deleteAllSpendings() {
        spendingInteractor.clearSpendings()
    }

    fun exportDatabase() {
        backupingInteractor.exportSpendings().subscribe({ getScreen().showToast("Database exported") }, this::onError)
    }

    fun onImportDatabase(importType: MainScreen.SpendingsImportType) {
        val directory = Environment.getExternalStorageDirectory().path + "/CanIBuyThat/"
        getScreen().showFilePickerActivity(directory, importType)
    }

    fun onImportSpendings(path: String, importType: MainScreen.SpendingsImportType) {
        when (importType) {
            MainScreen.SpendingsImportType.ALL -> backupingInteractor.importAllSpendings(path).subscribe(this::fetchBalance, this::onError)
            MainScreen.SpendingsImportType.MONZO -> backupingInteractor.importMonzoSpendings(path).subscribe(this::fetchBalance, this::onError)
            MainScreen.SpendingsImportType.NON_MONZO -> backupingInteractor.importNonMonzoSpendings(path).subscribe(this::fetchBalance, this::onError)
        }
    }

    fun updateBalance() {
        getScreen().showBalanceUpdateDialog()
    }

    fun showEditorScreenForSpending(id: Int) {
        getScreen().showEditorScreen(id)
    }

    fun showEditorScreen() {
        getScreen().showEditorScreen(null)
    }
}
