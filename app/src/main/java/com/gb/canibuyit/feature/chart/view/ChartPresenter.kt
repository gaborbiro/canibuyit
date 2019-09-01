package com.gb.canibuyit.feature.chart.view

import com.gb.canibuyit.UserPreferences
import com.gb.canibuyit.base.view.BasePresenter
import com.gb.canibuyit.feature.chart.model.ChartInfo
import com.gb.canibuyit.feature.spending.data.SpendingInteractor
import com.gb.canibuyit.feature.spending.model.CycleSpending
import com.github.mikephil.charting.data.Entry
import java.math.BigDecimal
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject
import com.gb.canibuyit.util.sumBy as sumByBigDecimal

class ChartPresenter @Inject constructor(
    private val spendingInteractor: SpendingInteractor,
    private val userPreferences: UserPreferences) : BasePresenter() {

    private val screen: ChartScreen by screenDelegate()

    init {
        disposeOnDestroy(spendingInteractor.spendingSubject.subscribe({ lce ->
            if (lce.loading) screen.showProgress() else screen.hideProgress()
            if (!lce.loading && !lce.hasError()) {
                val spendings = lce.content!!
                val spendingIds = spendings.associateBy { it.id!! }

                val dataSetSelection = userPreferences.dataSetSelection

                val cycleSpendings =
                    spendings.mapNotNull { if (dataSetSelection[it.type.toString()] != false && it.cycleSpendings != null) it.cycleSpendings else null }
                        .flatMap { Iterable { it.iterator() } }
                if (cycleSpendings.isNotEmpty()) {
                    val monthMap =
                        cycleSpendings
                            .groupBy { it.to.year * 12 + it.to.month.value }
                            .mapValues {
                                it.value.groupBy { it.spendingId!! }.mapValues { it.value.sumByBigDecimal(CycleSpending::amount) }
                            }
                    val spendingMap =
                        cycleSpendings
                            .groupBy { it.spendingId!! }
                            .mapValues {
                                it.value.groupBy { it.to.year * 12 + it.to.month.value }
                                    .mapValues { it.value.sumByBigDecimal(CycleSpending::amount) }
                            }

                    val minMonth = monthMap.keys.min()!!
                    val maxMonth = monthMap.keys.max()!!

                    val dataSet: MutableMap<String, List<Entry>> = mutableMapOf()
                    spendingMap.forEach { (spendingId: Int, categorySpending: Map<Int, BigDecimal>) ->
                        val type = spendingIds[spendingId]!!.type.toString()
                        val entries = mutableListOf<Entry>()
                        categorySpending.forEach { (month: Int, amount: BigDecimal) ->
                            val value = -amount.toFloat()
                            val formattedValue = if (value < 0) "+${-value}" else value.toString()
                            val xValue = (month - minMonth).toFloat()
                            entries.add(
                                Entry(xValue, value,
                                    ChartInfo(infoPopupText = "$type: $formattedValue",
                                        pointLabel = "${type.substring((0..2))}: $formattedValue",
                                        spendigId = spendingId)))
                        }
                        dataSet[type] = entries
                    }

                    var previousMonth: Map<Int, BigDecimal>? = null
                    val totals = monthMap.map { (month: Int, typeSpendings: Map<Int, BigDecimal>) ->
                        val xValue = (month - minMonth).toFloat()
                        val breakdown = typeSpendings.entries.sortedBy { it.value }.map { (spendingId: Int, amount: BigDecimal) ->
                            val value = -amount.toFloat()
                            val spending = spendingIds[spendingId]!!
                            val valueStr = if (previousMonth != null && previousMonth!![spendingId] != null) {
                                val previousAmount = -previousMonth!![spendingId]!!.toFloat()
                                val diff = ((value - previousAmount) / previousAmount * 100).toInt()
                                if (diff > 0) {
                                    "$value (+$diff%)"
                                } else {
                                    "$value ($diff%)"
                                }
                            } else {
                                value.toString()
                            }
                            "${spending.type}: $valueStr"
                        }.joinToString(separator = "\n")
                        val value = -typeSpendings.values.sumByBigDecimal { it }.toFloat()
                        val formattedValue = if (value < 0) "+${-value}" else value.toString()
                        Entry(xValue, value,
                            ChartInfo(
                                infoPopupText = breakdown,
                                pointLabel = "Total: $formattedValue",
                                spendigId = -1)).also {
                            previousMonth = typeSpendings
                        }
                    }
                    val xAxisLabels = Array(maxMonth - minMonth + 1) {
                        Month.of((minMonth + it) % 12).getDisplayName(TextStyle.SHORT, Locale.getDefault())
                    }
                    screen.display(totals, dataSet, xAxisLabels)
                }
            }
        }, this::onError))
    }

    fun loadSpendings() {
        spendingInteractor.loadSpendings()
    }
}