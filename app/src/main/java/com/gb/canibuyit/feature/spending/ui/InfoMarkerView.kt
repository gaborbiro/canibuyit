package com.gb.canibuyit.feature.spending.ui

import android.annotation.SuppressLint
import android.content.Context
import com.gb.canibuyit.feature.spending.model.CycleSpending
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import com.github.mikephil.charting.utils.Utils
import kotlinx.android.synthetic.main.info_marker.view.*

@SuppressLint("ViewConstructor")
class InfoMarkerView(context: Context, layoutResource: Int) : MarkerView(context, layoutResource) {

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        (e?.data as? CycleSpending)?.let {
            content.text = Utils.formatNumber(it.amount.toFloat(), 0, true)
        }
        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        return MPPointF((-(width / 2)).toFloat(), (-height).toFloat())
    }
}
