package com.gb.canibuyit.di

import com.gb.canibuyit.App
import com.gb.canibuyit.fcm.MonzoDispatchInstanceIdService
import com.gb.canibuyit.fcm.MonzoDispatchMessagingService
import com.gb.canibuyit.ui.BalanceReadingInputDialog
import com.gb.canibuyit.ui.LoginActivity
import com.gb.canibuyit.ui.MainActivity
import com.gb.canibuyit.ui.SpendingEditorFragment
import com.gb.canibuyit.ui.SpendingListFragment

interface CanIBuyItGraph {
    fun inject(app: App)

    fun inject(mainActivity: MainActivity)

    fun inject(spendingEditorFragment: SpendingEditorFragment)

    fun inject(balanceReadingInputDialog: BalanceReadingInputDialog)

    fun inject(spendingListFragment: SpendingListFragment)

    fun inject(loginActivity: LoginActivity)

    fun inject(monzoDispatchInstanceIdService: MonzoDispatchInstanceIdService)

    fun inject(monzoDispatchMessagingService: MonzoDispatchMessagingService)
}
