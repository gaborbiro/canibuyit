package com.gb.canibuyit.feature.spending.data

import com.gb.canibuyit.feature.spending.model.CycleSpending
import com.gb.canibuyit.feature.spending.model.Saving
import com.gb.canibuyit.feature.spending.model.Spending
import com.gb.canibuyit.feature.spending.persistence.model.ApiCycleSpending
import com.gb.canibuyit.feature.spending.persistence.model.ApiSaving
import com.gb.canibuyit.feature.spending.persistence.model.ApiSpending
import com.gb.canibuyit.util.fromJson
import com.google.gson.Gson
import java.math.BigDecimal
import java.time.LocalDate
import javax.inject.Inject

class SpendingMapper @Inject constructor(private val gson: Gson) {

    fun map(apiSpending: ApiSpending): Spending {
        return Spending(
            id = apiSpending.id,
            name = apiSpending.name!!,
            notes = apiSpending.notes,
            type = apiSpending.type!!,
            value = apiSpending.value!!,
            fromStartDate = apiSpending.fromStartDate!!,
            fromEndDate = apiSpending.fromEndDate!!,
            occurrenceCount = apiSpending.occurrenceCount,
            cycleMultiplier = apiSpending.cycleMultiplier!!,
            cycle = apiSpending.cycle!!,
            sourceData = mapSourceData(apiSpending.sourceData),
            enabled = apiSpending.enabled!!,
            spent = apiSpending.spent ?: BigDecimal.ZERO,
            cycleSpendings = apiSpending.cycleSpendings?.map(this::map),
            targets = apiSpending.targets?.let {
                gson.fromJson<MutableMap<LocalDate, Int>>(it)
            },
            savings = apiSpending.savings?.map(this::map)?.toTypedArray()
        ).apply {
            if (savings?.isEmpty() == true) {
                savings = null
            }
        }
    }

    /**
     * Note, savings are omitted. Those need to be stored separately with the SavingRepository
     */
    fun map(spending: Spending): ApiSpending {
        return ApiSpending(
            id = spending.id,
            name = spending.name,
            notes = spending.notes,
            type = spending.type,
            value = spending.value,
            fromStartDate = spending.fromStartDate,
            fromEndDate = spending.fromEndDate,
            occurrenceCount = spending.occurrenceCount,
            cycleMultiplier = spending.cycleMultiplier,
            cycle = spending.cycle,
            sourceData = spending.sourceData?.let { gson.toJson(it) },
            enabled = spending.enabled,
            spent = spending.spent,
            targets = spending.targets?.let { gson.toJson(it) })
    }

    fun map(apiSpentByCycle: ApiCycleSpending): CycleSpending =
        CycleSpending(
            id = apiSpentByCycle.id!!,
            spendingId = apiSpentByCycle.spending?.id!!,
            from = apiSpentByCycle.from!!,
            to = apiSpentByCycle.to!!,
            amount = apiSpentByCycle.amount!!,
            target = apiSpentByCycle.target,
            count = apiSpentByCycle.count!!
        )

    fun map(cycleSpending: CycleSpending, apiSpending: ApiSpending): ApiCycleSpending =
        ApiCycleSpending(
            id = cycleSpending.id,
            spending = apiSpending,
            from = cycleSpending.from,
            to = cycleSpending.to,
            amount = cycleSpending.amount,
            target = cycleSpending.target,
            count = cycleSpending.count
        )

    fun map(apiSaving: ApiSaving): Saving {
        return Saving(
            id = apiSaving.id!!,
            spendingId = apiSaving.spending?.id!!,
            amount = apiSaving.amount!!,
            created = apiSaving.created!!,
            target = apiSaving.target!!)
    }

    fun map(saving: Saving, apiSpending: ApiSpending): ApiSaving {
        return ApiSaving(
            id = saving.id,
            spending = apiSpending,
            amount = saving.amount,
            created = saving.created,
            target = saving.target)
    }

    fun mapSourceData(sourceData: String?): Map<String, String>? {
        return sourceData?.let { gson.fromJson<Map<String, String>>(it) }
    }
}