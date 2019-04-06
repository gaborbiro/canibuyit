package com.gb.canibuyit.feature.spending.view

import com.gb.canibuyit.feature.spending.model.Spending
import com.gb.canibuyit.base.view.Screen

interface SpendingListScreen : Screen {

    fun setData(spendings: List<Spending>)
}
