package com.gb.canibuyit.feature.spending.ui

import android.annotation.SuppressLint
import android.content.Context
import com.gb.canibuyit.feature.spending.model.CycleSpending
import com.gb.canibuyit.util.ClickableMarkerView
import com.gb.canibuyit.util.reverseSign
import com.github.mikephil.charting.data.Entry

@SuppressLint("ViewConstructor")
class InfoMarkerView(context: Context) : ClickableMarkerView(context) {

    override fun text(e: Entry?) = (e?.data as? CycleSpending)?.amount?.toFloat()?.reverseSign() ?: ""
}
