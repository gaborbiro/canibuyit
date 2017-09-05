package com.gb.canibuythat.screen

import com.gb.canibuythat.model.Spending

interface SpendingListScreen : Screen {

    fun setData(spendings: List<Spending>)
}
