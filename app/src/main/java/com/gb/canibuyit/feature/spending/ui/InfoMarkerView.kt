package com.gb.canibuyit.feature.spending.ui

import android.annotation.SuppressLint
import android.content.Context
import com.gb.canibuyit.feature.spending.model.CycleSpending
import com.gb.canibuyit.util.ClickableMarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.utils.Utils

@SuppressLint("ViewConstructor")
class InfoMarkerView(context: Context) : ClickableMarkerView(context) {

    override fun text(e: Entry?) = (e?.data as? CycleSpending)?.let {
        Utils.formatNumber(it.amount.toFloat(), 0, true)
    } ?: ""
}
