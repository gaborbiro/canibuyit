package com.gb.canibuythat.presenter

import android.content.Intent
import com.gb.canibuythat.*
import com.gb.canibuythat.interactor.BackupingInteractor
import com.gb.canibuythat.interactor.MonzoInteractor
import com.gb.canibuythat.interactor.ProjectInteractor
import com.gb.canibuythat.interactor.SpendingInteractor
import com.gb.canibuythat.model.Balance
import com.gb.canibuythat.screen.MainScreen
import com.gb.canibuythat.util.DateUtils
import io.reactivex.functions.Consumer
import java.util.*
import javax.inject.Inject

class MainPresenter @Inject
constructor(val monzoInteractor: MonzoInteractor,
            val spendingInteractor: SpendingInteractor,
            val projectInteractor: ProjectInteractor,
            val backupingInteractor: BackupingInteractor,
            val credentialsProvider: CredentialsProvider,
            val userPreferences: UserPreferences) : BasePresenter<MainScreen>() {

    init {
        disposeOnFinish(spendingInteractor.getSpendingsDataStream().subscribe({
            if (it.loading) getScreen().showProgress() else getScreen().hideProgress()
            if (!it.loading && !it.hasError()) fetchBalance()
        }, this::onError))
        disposeOnFinish(monzoInteractor.getLoginDataStream().subscribe({
            if (it.loading) {
                getScreen().showProgress()
            } else {
                if (it.hasError()) {
                    this.onError(it.error!!)
                } else {
                    getScreen().hideProgress()
                    it.content!!.let {
                        credentialsProvider.accessToken = it.accessToken
                        credentialsProvider.accessTokenExpiry = it.expiresAt
                        credentialsProvider.refreshToken = it.refreshToken
                        getScreen().showToast("You are now logged in")
                    }
                }
            }
        }, this::onError))
    }

    override fun onScreenSet() {
        disposeOnFinish(userPreferences.getBalanceReadingDataStream().subscribe { fetchBalance() })
        disposeOnFinish(userPreferences.getEstimateDateDataStream().subscribe { fetchBalance() })
        getScreen().showToast("Session expiry: " + DateUtils.FORMAT_DATE_TIME.format(credentialsProvider.accessTokenExpiry))
    }

    private fun fetchBalance() {
        disposeOnFinish(spendingInteractor.getBalance()
                .doOnSubscribe { getScreen().showProgress() }
                .doAfterTerminate { getScreen().hideProgress() }
                .subscribe(getScreen()::setBalanceInfo, {
                    getScreen().setBalanceInfo(Balance())
                    this.onError(com.gb.canibuythat.exception.DomainException("Cannot calculate balance. See logs", it))
                }))
        projectInteractor.getProject().subscribe(Consumer {
            getScreen().setTitle(it.projectName)
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
        if (!credentialsProvider.isRefresh()) {
            getScreen().showLoginActivity()
        } else {
            disposeOnFinish(monzoInteractor.loadSpendings(listOf(MonzoConstants.ACCOUNT_ID_PREPAID, MonzoConstants.ACCOUNT_ID_RETAIL), SPENDINGS_MONTH_SPAN))
        }
    }

    fun deleteAllSpendings() {
        spendingInteractor.clearSpendings()
    }

    fun exportDatabase() {
        projectInteractor.getProject().subscribe(Consumer {
            getScreen().showPickerForExport(getSuggestedExportPath(it.projectName))
        })
    }

    fun onExportSpendings(path: String) {
        backupingInteractor.exportSpendings(path).subscribe({ getScreen().showToast("Database exported") }, this::onError)
    }

    private fun getSuggestedExportPath(projectName: String?): String {
        return if (projectName.isNullOrEmpty()) {
            BACKUP_FOLDER + "/spendings-" + DateUtils.FORMAT_ISO.format(Date()) + ".sqlite"
        } else {
            BACKUP_FOLDER + "/spendings-" + DateUtils.FORMAT_ISO.format(Date()) + "-" + projectName + ".sqlite"
        }
    }

    fun onImportDatabase(importType: MainScreen.SpendingsImportType) {
        val directory = BACKUP_FOLDER + "/";
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

    fun onSetProjectName() {
        projectInteractor.getProject().subscribe({
            getScreen().setProjectName(it.projectName)
        }, errorHandler::onErrorSoft)
    }

    fun setProjectName(projectName: String) {
        projectInteractor.getProject().subscribe({
            it.projectName = projectName
            getScreen().showToast("Project name saved")
            getScreen().setTitle(projectName)
        }, errorHandler::onErrorSoft)
    }
}
