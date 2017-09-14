package com.gb.canibuythat.screen

import com.gb.canibuythat.model.Balance
import com.gb.canibuythat.model.Spending

interface MainScreen : ProgressScreen {

    enum class SpendingsImportType {
        MONZO,
        NON_MONZO,
        ALL
    }

    fun showChartScreen()

    fun showLoginActivity()

    fun setBalanceInfo(balance: Balance)

    fun showPickerForImport(directory: String, spendingsImportType: SpendingsImportType)

    fun showBalanceUpdateDialog()

    fun showEditorScreen(spendingId: Int?)

    fun showToast(message: String)

    fun showCategoryBalance(text: String)

    fun setProjectName(name: String?)

    fun showPickerForExport(suggestedPath: String)
}
