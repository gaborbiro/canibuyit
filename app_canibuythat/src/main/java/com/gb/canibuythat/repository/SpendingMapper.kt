package com.gb.canibuythat.repository

import com.gb.canibuythat.db.model.ApiSpending
import com.gb.canibuythat.exception.MapperException
import com.gb.canibuythat.model.Spending
import com.gb.canibuythat.util.fromJson
import com.google.gson.Gson
import java.util.*
import javax.inject.Inject

class SpendingMapper @Inject constructor(private val savingMapper: SavingMapper,
                                         private val gson: Gson) {
    fun map(apiSpending: ApiSpending): Spending {
        return Spending(
                id = apiSpending.id,
                name = apiSpending.name ?: throw MapperException("Missing name"),
                notes = apiSpending.notes,
                type = apiSpending.type ?: throw MapperException("Missing type"),
                value = apiSpending.value ?: throw MapperException("Missing value"),
                fromStartDate = apiSpending.fromStartDate ?: throw MapperException("Missing fromStartDate"),
                fromEndDate = apiSpending.fromEndDate ?: throw MapperException("Missing fromEndDate"),
                occurrenceCount = apiSpending.occurrenceCount,
                cycleMultiplier = apiSpending.cycleMultiplier ?: throw MapperException("Missing cycleMultiplier"),
                cycle = apiSpending.cycle ?: throw MapperException("cycle"),
                sourceData = apiSpending.sourceData?.let { gson.fromJson<MutableMap<String, String>>(it) },
                enabled = apiSpending.enabled ?: throw MapperException("Missing enabled"),
                spent = apiSpending.spent,
                targets = apiSpending.targets?.let { gson.fromJson<MutableMap<Date, Double>>(it) },
                savings = apiSpending.savings?.map(savingMapper::mapApiSaving)?.toTypedArray()
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
}