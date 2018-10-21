package com.gb.canibuyit.screen

import com.gb.canibuyit.model.Spending

interface SpendingListScreen : Screen {

    fun setData(spendings: List<Spending>)
}
