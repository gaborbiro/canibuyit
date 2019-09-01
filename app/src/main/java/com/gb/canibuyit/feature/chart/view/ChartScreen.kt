package com.gb.canibuyit.feature.chart.view

import com.gb.canibuyit.base.view.ProgressScreen
import com.github.mikephil.charting.data.Entry

interface ChartScreen : ProgressScreen {
    fun display(total: List<Entry>, dataSet: Map<String, List<Entry>>, xAxisLabels: Array<String>)
}