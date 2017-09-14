package com.gb.canibuythat.presenter

import android.content.Intent
import com.gb.canibuythat.AppConstants
import com.gb.canibuythat.CredentialsProvider
import com.gb.canibuythat.MonzoConstants
import com.gb.canibuythat.UserPreferences
import com.gb.canibuythat.interactor.BackupingInteractor
import com.gb.canibuythat.interactor.MonzoDispatchInteractor
import com.gb.canibuythat.interactor.MonzoInteractor
import com.gb.canibuythat.interactor.SpendingInteractor
import com.gb.canibuythat.model.Balance
import com.gb.canibuythat.screen.MainScreen
import io.reactivex.functions.Consumer
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class MainPresenter @Inject
constructor(val monzoInteractor: MonzoInteractor,
            val spendingInteractor: SpendingInteractor,
            val backupingInteractor: BackupingInteractor,
            val monzoDispatchInteractor: MonzoDispatchInteractor,
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
        spendingInteractor.getProjectName().subscribe(Consumer {
            getScreen().setProjectName(it.content)
        })
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
        getScreen().showPickerForExport(getSuggestedExportPath())
    }

    fun onExportSpendings(path: String) {
        backupingInteractor.exportSpendings(path).subscribe({ getScreen().showToast("Database exported") }, this::onError)
    }

    private fun getSuggestedExportPath(): String {
        val sdf = SimpleDateFormat("yyyyMMdd'T'HHmmssZ")
        return AppConstants.BACKUP_FOLDER
    }

    fun onImportDatabase(importType: MainScreen.SpendingsImportType) {
        val directory = AppConstants.BACKUP_FOLDER + "/";
        getScreen().showPickerForImport(directory, importType)
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

    fun sendFCMTokenToServer(token: String) {
        monzoDispatchInteractor.register(token)
                .subscribe({ dispatchRegistration ->
                    monzoInteractor.getWebhooks(MonzoConstants.ACCOUNT_ID).subscribe({
                        it.webhooks.forEach { monzoInteractor.deleteWebhook(it).subscribe() }
                        monzoInteractor.registerWebhook(MonzoConstants.ACCOUNT_ID, MonzoConstants.MONZO_DISPATCH_API_WEBHOOK + "/" + dispatchRegistration.hash).subscribe({
                            getScreen().showToast("Successfully registered for Monzo push notifications " + MonzoConstants.MONZO_DISPATCH_API_WEBHOOK + "/" + dispatchRegistration.hash)
                        }, {
                            errorHandler.onErrorSoft(it)
                        })
                    }, {
                        errorHandler.onErrorSoft(it)
                    })
                }, {
                    errorHandler.onErrorSoft(it)
                })
    }
}
