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
                var minMonth = Int.MAX_VALUE
                var maxMonth = Int.MIN_VALUE

                class SpendingCalendar(
                    val spendingId: Int,
                    val spendingsByMonth: SparseArray<CycleSpending> // using sparse array because there may be months in which nothing in given category was spent
                )

                val entryMap: MutableMap<String, SpendingCalendar> = mutableMapOf()
//                val totalMap: MutableMap<Int, BigDecimal> = mutableMapOf()
                val dataSetSelection = userPreferences.dataSetSelection
                spendings?.filter { it.enabled && dataSetSelection[it.type.toString()] != false }?.forEach { spending ->
                    val spendingsByMonth: SparseArray<CycleSpending> = SparseArray()
                    spending.cycleSpendings?.forEach { spending ->
                        val month = spending.from.year * 12 + spending.from.month.value
                        spendingsByMonth.put(month, spending)
                        minMonth = min(minMonth, month)
                        maxMonth = max(maxMonth, month)
//                        totalMap[index] = it.amount + (totalMap[index] ?: BigDecimal.ZERO)
                    }
                    entryMap[spending.type.toString()] = SpendingCalendar(spending.id!!, spendingsByMonth)
                }
                var minValue = Float.MAX_VALUE
                var maxValue = Float.MIN_VALUE
                val dataSet = entryMap.mapValues { mapEntry ->
                    val result: MutableList<Entry> = mutableListOf()
                    mapEntry.value.spendingsByMonth.keyIterator().forEach {
                        val value = -mapEntry.value.spendingsByMonth[it].amount.toFloat()
                        result.add(Entry((it - minMonth).toFloat(), value, Pair(mapEntry.key, mapEntry.value.spendingId)))
                        minValue = min(minValue, value)
                        maxValue = max(maxValue, value)
                    }
                    result
                }
//                var minTotal = Float.MAX_VALUE
//                var maxTotal = Float.MIN_VALUE
//                totalMap.values.forEach {
//                    val value = -it.toFloat()
//                    minTotal = min(minTotal, value)
//                    maxTotal = max(maxTotal, value)
//                }
//                val offset = minValue - minTotal
//                val multiplier = (maxValue - minValue) / (maxTotal - minTotal)
//                val total = totalMap.map {
//                    Entry((it.key - minIndex).toFloat(), -it.value.toFloat(), Pair("Total", null))
////                    Entry((it.key - minIndex).toFloat(), (-it.value.toFloat() + offset) * multiplier, Pair("Total", null))
//                }
                val xAxisLabels = Array(maxMonth - minMonth + 1) {
                    Month.of((minMonth + it) % 12).getDisplayName(TextStyle.SHORT, Locale.getDefault())
                }
                screen.display(dataSet, xAxisLabels)
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