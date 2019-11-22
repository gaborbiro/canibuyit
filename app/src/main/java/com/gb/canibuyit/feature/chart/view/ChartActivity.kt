package com.gb.canibuyit.feature.chart.view

import android.app.Activity
import android.content.Intent
import android.graphics.Rect
import android.graphics.Typeface
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.CheckBox
import androidx.annotation.ColorInt
import com.gb.canibuyit.R
import com.gb.canibuyit.UserPreferences
import com.gb.canibuyit.base.view.BaseActivity
import com.gb.canibuyit.di.Injector
import com.gb.canibuyit.feature.chart.model.ChartInfo
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
import kotlin.math.roundToInt

class ChartActivity : BaseActivity(), ChartScreen, OnChartValueSelectedListener {
    @Inject
    internal lateinit var presenter: ChartPresenter
    @Inject
    internal lateinit var userPreferences: UserPreferences

    private val dataSetMap: MutableMap<String, Pair<Int, LineDataSet>> = mutableMapOf()
    private lateinit var chartColors: IntArray
    private var selectedSpendingId: Int = -1
    private var primaryTextColor: Int = -1
    private var totalDataSet: LineDataSet? = null

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
        primaryTextColor = themeAttributeToColor(android.R.attr.textColorPrimary, R.color.black_100)
        chart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            setOnChartValueSelectedListener(this@ChartActivity)
            setDrawGridBackground(false)
            val mv = InfoMarkerView(context)
            mv.isClickable = true
            mv.chartView = this
            marker = mv
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            xAxis.setDrawGridLines(false)
            legend.isEnabled = false
            axisLeft.apply {
                setDrawGridLines(true)
            }
            axisRight.isEnabled = false
            setDrawBorders(false)
            onChartGestureListener = object : OnChartGestureListenerAdapter() {

                override fun onChartSingleTapped(me: MotionEvent) {
                    super.onChartSingleTapped(me)
                    val infoMarker = marker as InfoMarkerView
                    if (selectedSpendingId > -1 && infoMarker.visibility == View.VISIBLE) {
                        val rect =
                            Rect(infoMarker.realLeft.toInt(), infoMarker.realTop.toInt(), infoMarker.realLeft.toInt() + infoMarker.width,
                                infoMarker.realTop.toInt() + infoMarker.height)
                        if (rect.contains(me.x.toInt(), me.y.toInt())) {
                            startActivity(SpendingEditorActivity.getIntentForUpdate(context, selectedSpendingId))
                        }
                    }
                }

                override fun onChartGestureEnd(me: MotionEvent, lastPerformedGesture: ChartTouchListener.ChartGesture) {
                    if (lastPerformedGesture != ChartTouchListener.ChartGesture.SINGLE_TAP) {
                        highlightValues(null)
                    }
                }
            }
        }
        presenter.loadSpendings()
    }

    override fun display(dataSet: Map<String, List<Entry>>, xAxisLabels: Array<String>) {
        chart.data = LineData()
        dataSetMap.clear()
        val dataSetSelection = userPreferences.dataSetSelection
        val filteredTotals = dataSet.filter { dataSetSelection.getOrDefault(it.key, false) }

        class ChartInfoCollector(
            val popupCollector: StringBuilder = StringBuilder(),
            var total: Float = 0f
        )

        val totalsMap = mutableMapOf<Float, ChartInfoCollector>()

        filteredTotals.flatMap { entry ->
            entry.value.map { Triple(it.x, entry.key, it.y) }
        }.forEach {
            totalsMap[it.first] = (totalsMap[it.first] ?: ChartInfoCollector()).apply {
                popupCollector.append("${it.second}: ${it.third}\n")
                total += it.third
            }
        }
        val totals = totalsMap
            .map {
                val x = it.key
                val y = it.value.total
                val info = ChartInfo(
                    infoPopupText = it.value.popupCollector.toString(),
                    pointLabel = it.value.total.roundToInt().toString(),
                    spendigId = -1
                )
                Entry(x, y, info)
            }
        totalDataSet = LineDataSet(totals, "Total")
            .apply {
                val color = formatLineDataSet(this, 0, 3f)
                dataSetMap["TOTALS"] = Pair(color, this)
            }
        if (dataSetSelection.getOrDefault("TOTALS", true)) {
            chart.lineData.addDataSet(totalDataSet)
        }
        dataSet.keys.forEachIndexed { index, type ->
            dataSet[type]?.let {
                LineDataSet(it, type).apply {
                    val color = formatLineDataSet(this, index + 1)
                    dataSetMap[type] = Pair(color, this)
                    if (userPreferences.dataSetSelection.getOrDefault(type, true)) {
                        chart.lineData.addDataSet(this)
                    }
                }
            }
        }
        with(chart) {
            xAxis.valueFormatter = IndexAxisValueFormatter(xAxisLabels)
            xAxis.axisMaximum = lineData.xMax + 0.5f
            xAxis.axisMinimum = lineData.xMin - 0.5f
            notifyDataSetChanged()
            invalidate()
            refreshDrawableState()
        }
        categories_container.removeAllViews()
        addCheckBox("TOTALS", "Total", true)
        ApiSpending.Category.values().filter { dataSet.keys.contains(it.toString()) }.forEach { category ->
            addCheckBox(category.toString(), category.toString(), true)
        }
        ApiSpending.Category.values().filter { !dataSet.keys.contains(it.toString()) }.forEach { category ->
            addCheckBox(category.toString(), category.toString(), false)
        }
    }

    private fun addCheckBox(key: String, value: String, bold: Boolean) {
        categories_container.add<CheckBox>(R.layout.list_item_chart).also {
            it.text = value
            it.setTypeface(null, if (bold) Typeface.BOLD else Typeface.NORMAL)
            it.setTextColor(dataSetMap[key]?.first ?: primaryTextColor)
            it.isChecked = userPreferences.dataSetSelection.getOrDefault(key, true)
            it.setOnCheckedChangeListener { _, isChecked ->
                userPreferences.dataSetSelection[key] = isChecked
                presenter.loadSpendings()
            }
        }
    }

    @ColorInt
    private fun formatLineDataSet(lineDataSet: LineDataSet, index: Int, lineWidth: Float = 1f): Int {
        val colorInt = chartColors[index % chartColors.size]
        lineDataSet.apply {
            setDrawIcons(false)
            color = colorInt
            this.lineWidth = lineWidth
            circleRadius = 3f
            circleColors = listOf(colorInt)
            setDrawCircleHole(true)
            circleRadius = 5f
            circleHoleRadius = 2.5f
            valueTextSize = 9f
            setDrawFilled(false)
            isHighlightEnabled = true
            valueFormatter = object : ValueFormatter() {
                override fun getPointLabel(entry: Entry): String {
                    return (entry.data as? ChartInfo)?.pointLabel ?: ""
                }
            }
            return colorInt
        }
    }

    override fun onNothingSelected() {
        chart.highlightValues(null)
        selectedSpendingId = -1
    }

    override fun onValueSelected(e: Entry?, h: Highlight?) {
        selectedSpendingId = (e?.data as? ChartInfo)?.spendigId ?: -1
    }

    companion object {
        fun launch(parent: Activity) {
            Intent(parent, ChartActivity::class.java).also(parent::startActivity)
        }
    }
}