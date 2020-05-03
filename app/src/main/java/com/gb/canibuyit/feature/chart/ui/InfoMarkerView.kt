package com.gb.canibuyit.feature.chart.ui

import android.content.Context
import com.gb.canibuyit.feature.chart.model.PointInfo
import com.gb.canibuyit.util.ClickableMarkerView
import com.github.mikephil.charting.data.Entry

class InfoMarkerView(context: Context) : ClickableMarkerView(context) {

    override fun text(e: Entry?) = (e?.data as? PointInfo)?.infoPopupText ?: ""
}