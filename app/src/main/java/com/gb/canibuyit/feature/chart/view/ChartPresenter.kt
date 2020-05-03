package com.gb.canibuyit.feature.chart.view

import com.gb.canibuyit.base.view.BasePresenter
import com.gb.canibuyit.feature.chart.model.PointInfo
import com.gb.canibuyit.feature.spending.data.SpendingInteractor
import com.gb.canibuyit.feature.spending.model.CycleSpending
import com.github.mikephil.charting.data.Entry
import java.math.BigDecimal
import java.time.Month
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.*
import javax.inject.Inject
import com.gb.canibuyit.util.sumBy as sumByBigDecimal

class ChartPresenter @Inject constructor(private val spendingInteractor: SpendingInteractor) : BasePresenter() {

    private val screen: ChartScreen by screenDelegate()

    init {
        disposeOnDestroy(spendingInteractor.spendingSubject.subscribe({ lce ->
            if (lce.loading) screen.showProgress() else screen.hideProgress()
            if (!lce.loading && !lce.hasError()) {
                val spendings = lce.content!!
                val spendingIds = spendings.associateBy { it.id!! }

                val cycleSpendings = spendings.mapNotNull { it.cycleSpendings }.flatMap { Iterable { it.iterator() } }
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
                                    .mapValues {
                                        val date = with(it.value[0].to) {
                                            YearMonth.of(year, month)
                                        }
                                        val sum = it.value.sumByBigDecimal(CycleSpending::amount)
                                        sum to date
                                    }
                            }

                    val minMonth = monthMap.keys.min()!!
                    val maxMonth = monthMap.keys.max()!!

                    val dataSet: MutableMap<String, List<Entry>> = mutableMapOf() // Category -> [month, spending]
                    spendingMap.forEach { (spendingId: Int, categorySpending: Map<Int, Pair<BigDecimal, YearMonth>>) ->
                        val type = spendingIds[spendingId]!!.type.label
                        val entries = mutableListOf<Entry>()
                        categorySpending.forEach { (months: Int, data: Pair<BigDecimal, YearMonth>) ->
                            val (amount, date) = data
                            val value = -amount.toInt()
                            val formattedValue = if (value < 0) "+${-value}" else value.toString()
                            val xValue = (months - minMonth).toFloat()
                            val info = PointInfo(
                                infoPopupText = "${type}: $formattedValue",
                                pointLabel = "${type.substring((0..2))}: $formattedValue",
                                spendigId = spendingId,
                                date = date
                            )
                            entries.add(Entry(xValue, -amount.toFloat(), info))
                        }
                        dataSet[type] = entries
                    }

                    val xAxisLabels = Array(maxMonth - minMonth + 1) {
                        Month.of((minMonth + it) % 12 + 1).getDisplayName(TextStyle.SHORT, Locale.getDefault())
                    }
                    screen.display(dataSet, xAxisLabels)
                }
            }
        }, this::onError))
    }

    fun loadSpendings() {
        spendingInteractor.loadSpendings()
    }
}