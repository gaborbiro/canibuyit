package com.gb.canibuyit.feature.spending.view

import com.gb.canibuyit.base.view.Screen
import com.gb.canibuyit.feature.spending.model.Spending

interface SpendingListScreen : Screen {

    fun setData(spendings: List<Spending>)
}
