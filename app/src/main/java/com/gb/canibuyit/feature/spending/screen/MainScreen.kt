package com.gb.canibuyit.feature.spending.screen

import com.gb.canibuyit.feature.spending.data.Balance
import com.gb.canibuyit.feature.spending.ui.BalanceBreakdown
import com.gb.canibuyit.screen.ProgressScreen

interface MainScreen : ProgressScreen {

    enum class SpendingsImportType {
        MONZO,
        NON_MONZO,
        ALL
    }

    fun close()

    fun showLoginActivity()

    fun setBalanceInfo(balance: Balance?)

    fun showPickerForImport(directory: String, spendingsImportType: SpendingsImportType)

    fun showBalanceUpdateDialog()

    fun showEditorScreen(spendingId: Int?)

    fun showDialog(title: String, text: String)

    fun setTitle(projectName: String?)

    fun showPickerForExport(suggestedPath: String)

    fun setProjectName(currentName: String?)

    fun showBalanceBreakdown(breakdown: BalanceBreakdown)

    fun sendFCMTokenToServer()

    fun setLastUpdate(lastUpdate: String)

    fun showToast(message: String)
}
