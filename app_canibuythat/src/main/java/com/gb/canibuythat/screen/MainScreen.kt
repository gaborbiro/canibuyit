package com.gb.canibuythat.screen

import com.gb.canibuythat.model.Balance
import com.gb.canibuythat.model.BudgetItem

interface MainScreen : Screen {
    fun showChartScreen()

    fun showLoginActivity()

    fun setBalanceInfo(balance: Balance)

    fun showFilePickerActivity(directory: String)

    fun showBalanceUpdateDialog()

    fun showEditorScreen(budgetItemId: Int?)

    fun setData(budgetItems: List<BudgetItem>)
}
