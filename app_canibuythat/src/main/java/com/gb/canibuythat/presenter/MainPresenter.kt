package com.gb.canibuythat.presenter

import android.content.Intent
import android.os.Environment
import android.text.TextUtils
import android.widget.Toast
import com.gb.canibuythat.CredentialsProvider
import com.gb.canibuythat.MonzoConstants
import com.gb.canibuythat.interactor.BudgetInteractor
import com.gb.canibuythat.interactor.MonzoInteractor
import com.gb.canibuythat.model.BudgetItem
import com.gb.canibuythat.screen.MainScreen
import javax.inject.Inject

class MainPresenter @Inject
constructor(private val monzoInteractor: MonzoInteractor,
            private val budgetInteractor: BudgetInteractor,
            private val credentialsProvider: CredentialsProvider) : BasePresenter<MainScreen>() {

    fun fetchBalance() {
        budgetInteractor.calculateBalance()
                .doOnSubscribe { screen.showProgress() }
                .doAfterTerminate { screen.hideProgress() }
                .subscribe(screen::setBalanceInfo, this::onError)
    }

    fun handleDeepLink(intent: Intent) {
        val data = intent.data
        if (data.authority == MonzoConstants.MONZO_AUTH_AUTHORITY) {
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
        monzoInteractor.login(authorizationCode)
                .doOnSubscribe { screen.showProgress() }
                .doAfterTerminate { screen.hideProgress() }
                .subscribe({ login ->
                    credentialsProvider.accessToken = login.accessToken
                    credentialsProvider.refreshToken = login.refreshToken
                    screen.showToast("You are now logged in")
                }, this::onError)
    }

    fun chartButtonClicked() {
        screen.showChartScreen()
    }

    fun loadMonzoData() {
        if (TextUtils.isEmpty(credentialsProvider.accessToken)) {
            screen.showLoginActivity()
        } else {
            monzoInteractor.getBudgetItems(MonzoConstants.ACCOUNT_ID)
                    .doOnSubscribe { screen.showProgress() }
                    .doAfterTerminate { screen.hideProgress() }
                    .subscribe(this::onTransactionsLoaded, this::onError)
        }
    }

    fun onTransactionsLoaded(budgetItems: List<BudgetItem>) {
        budgetInteractor.createOrUpdateMonzoCategories(budgetItems)
                .subscribe({
                    budgetInteractor.all.subscribe({
                        screen.setData(it)
                    }, {
                        this.onError(it)
                    })
                }, this::onError)
    }


    fun exportDatabase() {
        budgetInteractor.exportDatabase().subscribe(this::onImportDatabase, this::onError)
    }

    fun onImportDatabase() {
        val directory = Environment.getExternalStorageDirectory().path + "/CanIBuyThat/"
        screen.showFilePickerActivity(directory)
    }

    fun onDatabaseFileSelected(path: String) {
        budgetInteractor.importDatabase(path).subscribe(this::fetchBalance, this::onError)
    }

    fun updateBalance() {
        screen.showBalanceUpdateDialog()
    }

    fun showEditorScreenForBudgetItem(id: Int) {
        screen.showEditorScreen(id)
    }

    fun showEditorScreen() {
        screen.showEditorScreen(null)
    }
}
