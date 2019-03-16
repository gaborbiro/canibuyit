package com.gb.canibuyit.feature.spending.presenter

import android.content.Intent
import com.gb.canibuyit.feature.monzo.ACCOUNT_ID_RETAIL
import com.gb.canibuyit.BACKUP_FOLDER
import com.gb.canibuyit.feature.monzo.CredentialsProvider
import com.gb.canibuyit.feature.monzo.MONZO_AUTH_AUTHORITY
import com.gb.canibuyit.feature.monzo.MONZO_AUTH_PATH_BASE
import com.gb.canibuyit.feature.monzo.MONZO_AUTH_PATH_CALLBACK
import com.gb.canibuyit.feature.monzo.MONZO_OAUTH_PARAM_AUTHORIZATION_CODE
import com.gb.canibuyit.feature.monzo.TRANSACTION_HISTORY_LENGTH_MONTHS
import com.gb.canibuyit.UserPreferences
import com.gb.canibuyit.feature.spending.persistence.model.ApiSpending
import com.gb.canibuyit.error.DomainException
import com.gb.canibuyit.feature.backup.data.BackupingInteractor
import com.gb.canibuyit.feature.monzo.data.MonzoInteractor
import com.gb.canibuyit.feature.project.data.ProjectInteractor
import com.gb.canibuyit.feature.spending.data.SpendingInteractor
import com.gb.canibuyit.presenter.BasePresenter
import com.gb.canibuyit.feature.spending.screen.MainScreen
import com.gb.canibuyit.util.Logger
import com.gb.canibuyit.util.formatSimpleDateTime
import io.reactivex.functions.Consumer
import java.time.LocalDateTime
import javax.inject.Inject

class MainPresenter @Inject
constructor(private val monzoInteractor: MonzoInteractor,
            private val spendingInteractor: SpendingInteractor,
            private val projectInteractor: ProjectInteractor,
            private val backupingInteractor: BackupingInteractor,
            private val credentialsProvider: CredentialsProvider,
            private val userPreferences: UserPreferences) : BasePresenter<MainScreen>() {

    init {
        disposeOnFinish(spendingInteractor.subscribeToSpendings({ lce ->
            if (lce.loading) getScreen().showProgress() else getScreen().hideProgress()
            if (!lce.loading && !lce.hasError()) {
                fetchBalance()
            }
        }, this::onError))
        disposeOnFinish(monzoInteractor.subscribeToLogin({ lce ->
            if (lce.loading) {
                getScreen().showProgress()
            } else {
                getScreen().hideProgress()
                if (lce.hasError()) {
                    this.onError(lce.error!!)
                } else {
                    lce.content?.let { login ->
                        credentialsProvider.accessToken = login.accessToken
                        credentialsProvider.accessTokenExpiry = login.expiresAt
                        credentialsProvider.refreshToken = login.refreshToken
                        getScreen().showToast("You are now logged in. Registering for Monzo notifications...")
                        getScreen().sendFCMTokenToServer()
                        fetchMonzoData()
                    }
                }
            }
        }, this::onError))
    }

    override fun onScreenSet() {
        disposeOnFinish(userPreferences.getBalanceReadingDataStream().subscribe { fetchBalance() })
        disposeOnFinish(userPreferences.getEstimateDateDataStream().subscribe { fetchBalance() })
    }

    private fun fetchBalance() {
        disposeOnFinish(spendingInteractor.getBalance()
                .doOnSubscribe { getScreen().showProgress() }
                .doAfterTerminate {
                    getScreen().hideProgress()
                    getScreen().setLastUpdate(userPreferences.lastUpdate?.formatSimpleDateTime()
                            ?: "never")
                }
                .subscribe(getScreen()::setBalanceInfo,
                        {
                            getScreen().setBalanceInfo(null)
                            this.onError(DomainException("Cannot calculate balance. See logs", it))
                        }))
        projectInteractor.getProject().subscribe(Consumer {
            getScreen().setTitle(it.projectName)
        })
    }

    fun showBalanceBreakdown() {
        getScreen().showBalanceBreakdown(spendingInteractor.getBalanceBreakdown())
    }

    fun onBalanceBreakdownItemClicked(category: ApiSpending.Category) {
        val details = spendingInteractor.getBalanceBreakdownCategoryDetails(category)
        details?.let {
            getScreen().showDialog(category.name.toLowerCase().capitalize(), it)
        } ?: let { getScreen().showToast("Unavailable") }
    }

    fun showTargetBalanceBreakdown() {
        getScreen().showDialog("Target balance breakdown", spendingInteractor.getTargetBalanceBreakdown())
    }

    fun showTargetSavingBreakdown() {
        getScreen().showDialog("Saved by keeping targets", spendingInteractor.getTargetSavingBreakdown())
    }

    fun handleDeepLink(intent: Intent) {
        val data = intent.data
        if (data != null && data.authority == MONZO_AUTH_AUTHORITY) {
            val pathSegments = data.pathSegments

            if (pathSegments[0] == MONZO_AUTH_PATH_BASE) {
                if (pathSegments[1] == MONZO_AUTH_PATH_CALLBACK) {
                    // finished email authentication -> exchange code for auth token
                    val authorizationCode = data.getQueryParameter(MONZO_OAUTH_PARAM_AUTHORIZATION_CODE)
                    login(authorizationCode)
                }
            }
        } else if (!credentialsProvider.isRefresh()) {
            getScreen().close()
            getScreen().showLoginActivity()
        }
    }

    private fun login(authorizationCode: String) {
        disposeOnFinish(monzoInteractor.login(authorizationCode))
    }

    fun fetchMonzoData() {
        if (!credentialsProvider.isRefresh()) {
            getScreen().showLoginActivity()
        } else {
            disposeOnFinish(monzoInteractor.loadSpendings(ACCOUNT_ID_RETAIL, TRANSACTION_HISTORY_LENGTH_MONTHS))
        }
    }

    fun deleteAllSpendings() {
        spendingInteractor.clearSpendings()
        userPreferences.clear()
    }

    fun exportDatabase() {
        projectInteractor.getProject().subscribe(Consumer {
            getScreen().showPickerForExport(getSuggestedExportPath(it.projectName))
        })
    }

    fun onExportSpendings(path: String) {
        backupingInteractor.exportSpendings(path)
                .subscribe({ getScreen().showToast("Database exported") }, this::onError)
    }

    private fun getSuggestedExportPath(projectName: String?): String {
        return if (projectName.isNullOrEmpty()) {
            BACKUP_FOLDER + "/spendings-" + LocalDateTime.now() + ".sqlite"
        } else {
            BACKUP_FOLDER + "/spendings-" + LocalDateTime.now() + "-" + projectName + ".sqlite"
        }
    }

    fun onImportDatabase(importType: MainScreen.SpendingsImportType) {
        val directory = BACKUP_FOLDER + "/"
        getScreen().showPickerForImport(directory, importType)
    }

    fun onImportSpendings(path: String, importType: MainScreen.SpendingsImportType) {
        when (importType) {
            MainScreen.SpendingsImportType.ALL -> backupingInteractor.importAllSpendings(path).subscribe(this::fetchBalance, this::onError)
            MainScreen.SpendingsImportType.MONZO -> backupingInteractor.importMonzoSpendings(path).subscribe(this::fetchBalance, this::onError)
            MainScreen.SpendingsImportType.NON_MONZO -> backupingInteractor.importNonMonzoSpendings(path).subscribe(this::fetchBalance, this::onError)
        }
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

    fun logWebhooks() {
        monzoInteractor.getWebhooks(ACCOUNT_ID_RETAIL).subscribe({ retailWebhooks ->
            val buffer = StringBuffer()
            buffer.append("Retail account:\n")
            retailWebhooks.webhooks.joinTo(buffer, separator = "\n", transform = { it.url })
            Logger.d("com.gb.canibuyit.feature.spending.presenter.MainPresenter", buffer.toString())
            getScreen().showDialog("Webhooks", buffer.toString())
        }, errorHandler::onErrorSoft)
    }
}
