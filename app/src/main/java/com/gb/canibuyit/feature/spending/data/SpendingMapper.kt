package com.gb.canibuyit.feature.spending.data

import com.gb.canibuyit.error.MapperException
import com.gb.canibuyit.feature.spending.model.CycleSpent
import com.gb.canibuyit.feature.spending.model.Saving
import com.gb.canibuyit.feature.spending.model.Spending
import com.gb.canibuyit.feature.spending.persistence.model.ApiSaving
import com.gb.canibuyit.feature.spending.persistence.model.ApiSpending
import com.gb.canibuyit.feature.spending.persistence.model.ApiSpentByCycle
import com.gb.canibuyit.util.fromJson
import com.google.gson.Gson
import java.math.BigDecimal
import java.time.LocalDate
import javax.inject.Inject

class SpendingMapper @Inject constructor(private val gson: Gson) {

    fun map(apiSpending: ApiSpending): Spending {
        return Spending(
                id = apiSpending.id,
                name = apiSpending.name ?: throw MapperException("Missing name"),
                notes = apiSpending.notes,
                type = apiSpending.type ?: throw MapperException("Missing type"),
                value = apiSpending.value ?: throw MapperException("Missing value"),
                total = BigDecimal.ZERO,
                fromStartDate = apiSpending.fromStartDate ?: throw MapperException(
                        "Missing fromStartDate"),
                fromEndDate = apiSpending.fromEndDate ?: throw MapperException(
                        "Missing fromEndDate"),
                occurrenceCount = apiSpending.occurrenceCount,
                cycleMultiplier = apiSpending.cycleMultiplier ?: throw MapperException(
                        "Missing cycleMultiplier"),
                cycle = apiSpending.cycle ?: throw MapperException("cycle"),
                sourceData = mapSourceData(apiSpending.sourceData),
                enabled = apiSpending.enabled ?: throw MapperException("Missing enabled"),
                spent = apiSpending.spent ?: BigDecimal.ZERO,
                spentByCycle = apiSpending.spentByCycle?.map(this::map),
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

    fun map(apiSpentByCycle: ApiSpentByCycle): CycleSpent =
        CycleSpent(
                id = apiSpentByCycle.id ?: throw MapperException(
                        "Missing `id` when mapping ApiSpentByCycle"),
                spendingId = apiSpentByCycle.spending?.id ?: throw MapperException(
                        "Missing `spendingId` when mapping ApiSpentByCycle"),
                from = apiSpentByCycle.from ?: throw MapperException(
                        "Missing `from` when mapping ApiSpentByCycle"),
                to = apiSpentByCycle.to ?: throw MapperException(
                        "Missing `to` when mapping ApiSpentByCycle"),
                amount = apiSpentByCycle.amount ?: throw MapperException(
                        "Missing `amount` when mapping ApiSpentByCycle"),
                count = apiSpentByCycle.count ?: throw MapperException(
                        "Missing `count` when mapping ApiSpentByCycle")
        )

    fun map(cycleSpent: CycleSpent, apiSpending: ApiSpending): ApiSpentByCycle =
        ApiSpentByCycle(
                id = cycleSpent.id,
                spending = apiSpending,
                from = cycleSpent.from,
                to = cycleSpent.to,
                amount = cycleSpent.amount,
                count = cycleSpent.count
        )

    fun map(apiSaving: ApiSaving): Saving {
        return Saving(
                id = apiSaving.id ?: throw MapperException(
                        "Missing saving id when mapping ApiSaving"),
                spendingId = apiSaving.spending?.id ?: throw MapperException(
                        "Missing spending id when mapping ApiSaving"),
                amount = apiSaving.amount ?: throw MapperException(
                        "Missing amount when mapping ApiSaving"),
                created = apiSaving.created ?: throw MapperException(
                        "Missing created when mapping ApiSaving"),
                target = apiSaving.target ?: throw MapperException(
                        "Missing target when mapping ApiSaving"))
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