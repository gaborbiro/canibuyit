package com.gb.canibuyit.di

import com.gb.canibuyit.App
import com.gb.canibuyit.fcm.FirebaseInstanceIdService
import com.gb.canibuyit.fcm.PushMessagingFirebaseService
import com.gb.canibuyit.feature.spending.ui.BalanceReadingInputDialog
import com.gb.canibuyit.feature.monzo.ui.LoginActivity
import com.gb.canibuyit.feature.spending.ui.MainActivity
import com.gb.canibuyit.feature.spending.ui.SpendingEditorFragment
import com.gb.canibuyit.feature.spending.ui.SpendingListFragment

interface CanIBuyItGraph {
    fun inject(app: App)

    fun inject(mainActivity: MainActivity)

    fun inject(spendingEditorFragment: SpendingEditorFragment)

    fun inject(balanceReadingInputDialog: BalanceReadingInputDialog)

    fun inject(spendingListFragment: SpendingListFragment)

    fun inject(loginActivity: LoginActivity)

    fun inject(firebaseInstanceIdService: FirebaseInstanceIdService)

    fun inject(pushMessagingFirebaseService: PushMessagingFirebaseService)
}
