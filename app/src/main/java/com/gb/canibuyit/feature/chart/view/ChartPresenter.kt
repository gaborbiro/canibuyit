package com.gb.canibuyit.feature.chart.view

import android.util.SparseArray
import androidx.core.util.keyIterator
import com.gb.canibuyit.UserPreferences
import com.gb.canibuyit.base.view.BasePresenter
import com.gb.canibuyit.feature.spending.data.SpendingInteractor
import com.gb.canibuyit.feature.spending.model.CycleSpending
import com.github.mikephil.charting.data.Entry
import java.math.BigDecimal
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

class ChartPresenter @Inject constructor(
    private val spendingInteractor: SpendingInteractor,
    private val userPreferences: UserPreferences) : BasePresenter() {

    private val screen: ChartScreen by screenDelegate()

    init {
        disposeOnDestroy(spendingInteractor.spendingSubject.subscribe({ lce ->
            if (lce.loading) screen.showProgress() else screen.hideProgress()
            if (!lce.loading && !lce.hasError()) {
                val spendings = lce.content
                var minIndex = Int.MAX_VALUE
                var maxIndex = Int.MIN_VALUE
                val entryMap: MutableMap<String, Pair<SparseArray<CycleSpending>, Int>> = mutableMapOf()
                val totalsMap: MutableMap<Int, BigDecimal> = mutableMapOf()
                val chartSelection = userPreferences.chartSelection
                spendings?.filter { it.enabled && chartSelection[it.type.toString()] != false }?.forEach { spending ->
                    val entries: SparseArray<CycleSpending> = SparseArray()
                    spending.cycleSpendings?.forEach {
                        val index = it.from.year * 12 + it.from.month.value
                        entries.put(index, it)
                        minIndex = min(minIndex, index)
                        maxIndex = max(maxIndex, index)
                        totalsMap[index] = it.amount + (totalsMap[index] ?: BigDecimal.ZERO)
                    }
                    entryMap[spending.type.toString()] = Pair(entries, spending.id!!)
                }
                var minValue = Float.MAX_VALUE
                var maxValue = Float.MIN_VALUE
                val entries = entryMap.mapValues { mapEntry ->
                    val result: MutableList<Entry> = mutableListOf()
                    mapEntry.value.first.keyIterator().forEach {
                        val value = -mapEntry.value.first[it].amount.toFloat()
                        result.add(Entry((it - minIndex).toFloat(), value, Pair(mapEntry.key, mapEntry.value.second)))
                        minValue = min(minValue, value)
                        maxValue = max(maxValue, value)
                    }
                    result
                }
                var minTotal = Float.MAX_VALUE
                var maxTotal = Float.MIN_VALUE
                totalsMap.values.forEach {
                    val value = -it.toFloat()
                    minTotal = min(minTotal, value)
                    maxTotal = max(maxTotal, value)
                }
                val offset = minValue - minTotal
                val multiplier = (maxValue - minValue) / (maxTotal - minTotal)
                val totals = totalsMap.map {
                    Entry((it.key - minIndex).toFloat(), (-it.value.toFloat() + offset) * multiplier, Pair("Total", null))
                }
                val xAxisLabels = Array(maxIndex - minIndex + 1) {
                    Month.of((minIndex + it) % 12).getDisplayName(TextStyle.SHORT, Locale.getDefault())
                }
                screen.setEntries(totals, entries, minValue, maxValue, xAxisLabels)
            }
        }, this::onError))
    }

    private fun normalize(value: BigDecimal, min: Float, max: Float, lowerLimit: Float, upperLimit: Float): Float {
        return (value.toFloat() + (lowerLimit - min)) * ((upperLimit - lowerLimit) / (max - min))
    }

    fun loadSpendings() {
        spendingInteractor.loadSpendings()
    }
}