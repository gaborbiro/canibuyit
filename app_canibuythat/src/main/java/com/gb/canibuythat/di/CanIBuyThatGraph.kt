package com.gb.canibuythat.di

import com.gb.canibuythat.App
import com.gb.canibuythat.fcm.MonzoDispatchInstanceIdService
import com.gb.canibuythat.fcm.MonzoDispatchMessagingService
import com.gb.canibuythat.ui.BalanceReadingInputDialog
import com.gb.canibuythat.ui.ChartActivity
import com.gb.canibuythat.ui.LoginActivity
import com.gb.canibuythat.ui.MainActivity
import com.gb.canibuythat.ui.SpendingEditorFragment
import com.gb.canibuythat.ui.SpendingListFragment

interface CanIBuyThatGraph {
    fun inject(app: App)

    fun inject(mainActivity: MainActivity)

    fun inject(chartActivity: ChartActivity)

    fun inject(spendingEditorFragment: SpendingEditorFragment)

    fun inject(balanceReadingInputDialog: BalanceReadingInputDialog)

    fun inject(spendingListFragment: SpendingListFragment)

    fun inject(loginActivity: LoginActivity)

    fun inject(monzoDispatchInstanceIdService: MonzoDispatchInstanceIdService)

    fun inject(monzoDispatchMessagingService: MonzoDispatchMessagingService)
}
