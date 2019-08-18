package com.gb.canibuyit.feature.chart.view

import com.gb.canibuyit.base.view.ProgressScreen
import com.github.mikephil.charting.data.Entry

interface ChartScreen : ProgressScreen {
    fun setEntries(totals: List<Entry>, entries: Map<String, List<Entry>>, minValue: Float, maxValue: Float, xAxisLabels: Array<String>)
}