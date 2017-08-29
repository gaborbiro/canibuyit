package com.gb.canibuythat.screen

import com.gb.canibuythat.model.Balance
import com.gb.canibuythat.model.Spending

interface MainScreen : Screen {
    fun showChartScreen()

    fun showLoginActivity()

    fun setBalanceInfo(balance: Balance)

    fun showFilePickerActivity(directory: String)

    fun showBalanceUpdateDialog()

    fun showEditorScreen(spendingId: Int?)

    fun setData(spendings: List<Spending>)

    fun showToast(message: String)
}
