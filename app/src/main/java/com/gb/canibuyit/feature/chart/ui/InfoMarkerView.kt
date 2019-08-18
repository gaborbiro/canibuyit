package com.gb.canibuyit.feature.chart.ui

import android.annotation.SuppressLint
import android.content.Context
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import kotlinx.android.synthetic.main.info_marker.view.*

@SuppressLint("ViewConstructor")
class InfoMarkerView(context: Context, layoutResource: Int) : MarkerView(context, layoutResource) {

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        content.text = (e?.data as? Pair<*, *>)?.first.toString()
        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        return MPPointF((-(width / 2)).toFloat(), (-height).toFloat())
    }
}
