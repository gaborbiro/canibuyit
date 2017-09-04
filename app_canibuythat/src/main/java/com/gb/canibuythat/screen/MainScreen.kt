package com.gb.canibuythat.screen

import com.gb.canibuythat.model.Balance
import com.gb.canibuythat.model.Spending

interface MainScreen : ProgressScreen {
    fun showChartScreen()

    fun showLoginActivity()

    fun setBalanceInfo(balance: Balance)

    fun showFilePickerActivity(directory: String)

    fun showBalanceUpdateDialog()

    fun showEditorScreen(spendingId: Int?)

    fun showToast(message: String)
}
