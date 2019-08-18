package com.gb.canibuyit.feature.chart.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.widget.CheckBox
import androidx.annotation.ColorInt
import com.gb.canibuyit.R
import com.gb.canibuyit.UserPreferences
import com.gb.canibuyit.base.view.BaseActivity
import com.gb.canibuyit.di.Injector
import com.gb.canibuyit.feature.chart.ui.InfoMarkerView
import com.gb.canibuyit.feature.spending.persistence.model.ApiSpending
import com.gb.canibuyit.feature.spending.view.SpendingEditorActivity
import com.gb.canibuyit.util.OnChartGestureListenerAdapter
import com.gb.canibuyit.util.add
import com.gb.canibuyit.util.themeAttributeToColor
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.ChartTouchListener
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import kotlinx.android.synthetic.main.activity_chart.*
import javax.inject.Inject
import kotlin.math.min

class ChartActivity : BaseActivity(), ChartScreen, OnChartValueSelectedListener {
    @Inject internal lateinit var presenter: ChartPresenter
    @Inject internal lateinit var userPreferences: UserPreferences

    private val dataSetMap: MutableMap<String, Pair<Int, LineDataSet>> = mutableMapOf()
    private lateinit var chartColors: IntArray
    private var selectedSpendingId: Int = -1
    private lateinit var chartSelection: MutableMap<String, Boolean>
    private var primaryTextColor: Int = -1

    override fun inject() {
        Injector.INSTANCE.graph.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        presenter.screenReference = this
        setContentView(R.layout.activity_chart)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayShowTitleEnabled(true)
            setDisplayHomeAsUpEnabled(true)
            title = "Overview"
        }
        toolbar.setNavigationOnClickListener { finish() }
        chartColors = resources.getIntArray(R.array.chart_colors)
        chartSelection = userPreferences.chartSelection
        primaryTextColor = themeAttributeToColor(android.R.attr.textColorPrimary, R.color.black_100)
        chart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            setOnChartValueSelectedListener(this@ChartActivity)
            setDrawGridBackground(false)
            val mv = InfoMarkerView(context, R.layout.info_marker)
            mv.chartView = this
            marker = mv
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            xAxis.setDrawGridLines(false)
            legend.isEnabled = false
            axisLeft.apply {
                setDrawGridLines(false)
            }
            setDrawBorders(false)
            onChartGestureListener = object : OnChartGestureListenerAdapter() {

                override fun onChartGestureEnd(me: MotionEvent, lastPerformedGesture: ChartTouchListener.ChartGesture) {
                    if (lastPerformedGesture != ChartTouchListener.ChartGesture.SINGLE_TAP) {
                        highlightValues(null)
                    }
                }
            }
            axisRight.isEnabled = true
        }
        presenter.loadSpendings()
    }

    override fun setEntries(totals: List<Entry>, entries: Map<String, List<Entry>>, minValue: Float, maxValue: Float,
                            xAxisLabels: Array<String>) {
        chart.data = LineData()
        dataSetMap.clear()
        LineDataSet(totals, "Total").apply {
            val color = formatLineDataSet(this, 0)
            dataSetMap["TOTALS"] = Pair(color, this)
            if (userPreferences.chartSelection.getOrDefault("TOTALS", true)) {
                chart.lineData.addDataSet(this)
            }
        }
        entries.keys.forEachIndexed { index, name ->
            entries[name]?.let {
                LineDataSet(it, name).apply {
                    val color = formatLineDataSet(this, index + 1)
                    dataSetMap[name] = Pair(color, this)
                    if (userPreferences.chartSelection.getOrDefault(name, true)) {
                        chart.lineData.addDataSet(this)
                    }
                }
            }
        }
        with(chart) {
            xAxis.valueFormatter = IndexAxisValueFormatter(xAxisLabels)
            axisLeft.apply {
                axisMinimum = min(minValue * 1.2f, 0f)
                axisMaximum = maxValue * 1.2f
            }
            xAxis.axisMaximum = lineData.xMax + 0.5f
            xAxis.axisMinimum = lineData.xMin - 0.5f
            invalidate()
            refreshDrawableState()
        }
        addCheckBox("TOTALS", "Total", true)
        ApiSpending.Category.values().filter { entries.keys.contains(it.toString()) }.forEach { category ->
            addCheckBox(category.toString(), category.toString(), true)
        }
        ApiSpending.Category.values().filter { !entries.keys.contains(it.toString()) }.forEach { category ->
            addCheckBox(category.toString(), category.toString(), false)
        }
    }

    private fun addCheckBox(key: String, value: String, defaultSelection: Boolean) {
        categories_container.add<CheckBox>(R.layout.list_item_chart).also {
            it.text = value
            it.setTextColor(dataSetMap[key]?.first ?: primaryTextColor)
            it.isChecked = chartSelection.getOrDefault(key, defaultSelection)
            it.setOnCheckedChangeListener { _, isChecked ->
                if (chartSelection.getOrDefault(key, true)) {
                    if (!isChecked) {
                        chart.lineData.removeDataSet(dataSetMap[key]?.second)
                    }
                } else {
                    if (isChecked) {
                        chart.lineData.addDataSet(dataSetMap[key]?.second)
                    }
                }
                chartSelection[key] = isChecked
                with(chart) {
                    xAxis.axisMaximum = lineData.xMax + 0.5f
                    xAxis.axisMinimum = lineData.xMin - 0.5f
                    invalidate()
                    refreshDrawableState()
                }
            }
        }
    }

    @ColorInt
    private fun formatLineDataSet(lineDataSet: LineDataSet, index: Int): Int {
        val colorInt = chartColors[index % chartColors.size]
        lineDataSet.apply {
            setDrawIcons(false)
            color = colorInt
            lineWidth = 1f
            circleRadius = 3f
            circleColors = listOf(colorInt)
            setDrawCircleHole(true)
            circleRadius = 5f
            circleHoleRadius = 2.5f
            valueTextSize = 9f
            setDrawFilled(false)
            isHighlightEnabled = true
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
//                    return if (value < 0) "+${-value}" else value.toString()
                    return ""
                }
            }
            return colorInt
        }
    }

    override fun onNothingSelected() {
        chart.highlightValues(null)
        if (selectedSpendingId > -1) {
            startActivity(SpendingEditorActivity.getIntentForUpdate(this, selectedSpendingId))
            selectedSpendingId = -1
        }
    }

    override fun onValueSelected(e: Entry?, h: Highlight?) {
        selectedSpendingId = (e?.data as? Pair<*, *>)?.second as Int? ?: -1
    }

    companion object {
        fun launch(parent: Activity) {
            Intent(parent, ChartActivity::class.java).also(parent::startActivity)
        }
    }
}