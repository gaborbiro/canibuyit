package com.gb.canibuythat.repository

import com.gb.canibuythat.db.model.ApiSpending
import com.gb.canibuythat.exception.MapperException
import com.gb.canibuythat.model.Spending
import javax.inject.Inject

class SpendingMapper @Inject constructor() {
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
                sourceData = apiSpending.sourceData,
                enabled = apiSpending.enabled ?: throw MapperException("Missing enabled"),
                spent = apiSpending.spent,
                target = apiSpending.target,
                savings = apiSpending.savings
        )
    }

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
                sourceData = spending.sourceData,
                enabled = spending.enabled,
                spent = spending.spent,
                target = spending.target,
                savings = spending.savings)
    }
}