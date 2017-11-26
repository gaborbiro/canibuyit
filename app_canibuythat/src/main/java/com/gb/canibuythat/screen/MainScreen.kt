package com.gb.canibuythat.screen

import com.gb.canibuythat.model.Balance

interface MainScreen : MonzoDispatchScreen {

    enum class SpendingsImportType {
        MONZO,
        NON_MONZO,
        ALL
    }

    fun showLoginActivity()

    fun setBalanceInfo(balance: Balance?)

    fun showPickerForImport(directory: String, spendingsImportType: SpendingsImportType)

    fun showBalanceUpdateDialog()

    fun showEditorScreen(spendingId: Int?)

    fun showDialog(title: String, text: String)

    fun setTitle(projectName: String?)

    fun showPickerForExport(suggestedPath: String)

    fun setProjectName(currentName: String?)
}
