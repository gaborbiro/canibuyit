package com.gb.canibuyit.feature.spending.data

import com.gb.canibuyit.feature.spending.model.CycleSpending
import com.gb.canibuyit.feature.spending.model.Saving
import com.gb.canibuyit.feature.spending.model.Spending
import com.gb.canibuyit.feature.spending.persistence.model.DBCycleSpending
import com.gb.canibuyit.feature.spending.persistence.model.DBSaving
import com.gb.canibuyit.feature.spending.persistence.model.DBSpending
import com.gb.canibuyit.util.fromJson
import com.google.gson.Gson
import java.math.BigDecimal
import java.time.LocalDate
import javax.inject.Inject

class SpendingMapper @Inject constructor(private val gson: Gson) {

    fun map(dbSpending: DBSpending): Spending {
        return Spending(
            id = dbSpending.id,
            name = dbSpending.name!!,
            notes = dbSpending.notes,
            type = dbSpending.type!!,
            value = dbSpending.value!!,
            fromStartDate = dbSpending.fromStartDate!!,
            fromEndDate = dbSpending.fromEndDate!!,
            occurrenceCount = dbSpending.occurrenceCount,
            cycleMultiplier = dbSpending.cycleMultiplier!!,
            cycle = dbSpending.cycle!!,
            sourceData = mapSourceData(dbSpending.sourceData),
            enabled = dbSpending.enabled!!,
            spent = dbSpending.spent ?: BigDecimal.ZERO,
            cycleSpendings = dbSpending.cycleSpendings?.map(this::map),
            targets = dbSpending.targets?.let {
                gson.fromJson<MutableMap<LocalDate, Int>>(it)
            },
            savings = dbSpending.savings?.map(this::map)?.toTypedArray()
        ).apply {
            if (savings?.isEmpty() == true) {
                savings = null
            }
        }
    }

    /**
     * Note, savings are omitted. Those need to be stored separately with the SavingRepository
     */
    fun map(spending: Spending): DBSpending {
        return DBSpending(
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

    fun map(dbCycleSpending: DBCycleSpending): CycleSpending =
        CycleSpending(
            id = dbCycleSpending.id!!,
            spendingId = dbCycleSpending.spending?.id!!,
            from = dbCycleSpending.from!!,
            to = dbCycleSpending.to!!,
            amount = dbCycleSpending.amount!!,
            target = dbCycleSpending.target,
            count = dbCycleSpending.count!!
        )

    fun map(cycleSpending: CycleSpending, dbSpending: DBSpending): DBCycleSpending =
        DBCycleSpending(
            id = cycleSpending.id,
            spending = dbSpending,
            from = cycleSpending.from,
            to = cycleSpending.to,
            amount = cycleSpending.amount,
            target = cycleSpending.target,
            count = cycleSpending.count
        )

    fun map(dbSaving: DBSaving): Saving {
        return Saving(
            id = dbSaving.id!!,
            spendingId = dbSaving.spending?.id!!,
            amount = dbSaving.amount!!,
            created = dbSaving.created!!,
            target = dbSaving.target!!)
    }

    fun map(saving: Saving, dbSpending: DBSpending): DBSaving {
        return DBSaving(
            id = saving.id,
            spending = dbSpending,
            amount = saving.amount,
            created = saving.created,
            target = saving.target)
    }

    fun mapSourceData(sourceData: String?): Map<String, String>? {
        return sourceData?.let { gson.fromJson<Map<String, String>>(it) }
    }
}