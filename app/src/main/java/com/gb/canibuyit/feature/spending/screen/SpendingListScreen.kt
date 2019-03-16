package com.gb.canibuyit.feature.spending.screen

import com.gb.canibuyit.feature.spending.model.Spending
import com.gb.canibuyit.screen.Screen

interface SpendingListScreen : Screen {

    fun setData(spendings: List<Spending>)
}
