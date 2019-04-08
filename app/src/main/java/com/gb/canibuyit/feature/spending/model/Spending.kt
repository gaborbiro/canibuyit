package com.gb.canibuyit.feature.spending.model

import com.gb.canibuyit.feature.spending.persistence.model.ApiSpending
import com.gb.canibuyit.feature.spending.persistence.model.ApiSpending.Cycle.DAYS
import com.gb.canibuyit.feature.spending.persistence.model.ApiSpending.Cycle.MONTHS
import com.gb.canibuyit.feature.spending.persistence.model.ApiSpending.Cycle.WEEKS
import com.gb.canibuyit.feature.spending.persistence.model.ApiSpending.Cycle.YEARS
import com.gb.canibuyit.util.max
import com.gb.canibuyit.util.min
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters.lastDayOfMonth
import java.time.temporal.TemporalAdjusters.lastDayOfYear
import java.util.Arrays

class Spending(var id: Int? = null,
               var name: String,
               var notes: String? = null,
               var type: ApiSpending.Category,
               var value: BigDecimal,
               var total: BigDecimal,
               /**
                * Date before witch the transaction certainly won't happen. The repetition cycle
                * is added to this date.
                */
               var fromStartDate: LocalDate,
               var fromEndDate: LocalDate,
               var occurrenceCount: Int? = null,
               /**
                * For cycles like every 2 days or 2 weeks...
                */
               var cycleMultiplier: Int,
               /**
                * Does this modifier repeat every day/week/month/year. The first affected time-span
                * (specified by {@link Spending#fromStartDate} and {@link Spending#fromEndDate}) must not be larger
                * the this cycle.
                * Ex: The first week of every month, cold months of the year, every weekend, every
                * semester
                */
               var cycle: ApiSpending.Cycle,
               var enabled: Boolean,
               var sourceData: Map<String, String>? = null,
               var spent: BigDecimal,
               var cycleSpendings: List<CycleSpending>? = null,
               var targets: Map<LocalDate, Int>? = null,
               var savings: Array<out Saving>? = null) {

    val target = targets?.maxBy { it.key }?.value

    val valuePerMonth: BigDecimal
        get() {
            return value / cycle.toMonths().toBigDecimal() * cycleMultiplier.toBigDecimal()
        }

    fun compareForEditing(other: Any?, ignoreDates: Boolean,
                          ignoreCycleMultiplier: Boolean): ComparisonResult {
        if (this === other) return ComparisonResult.Different
        if (javaClass != other?.javaClass) return return ComparisonResult.Different

        other as Spending

        if (name != other.name) return ComparisonResult.Different
        if (notes != other.notes) return ComparisonResult.Different
        if (type != other.type) return ComparisonResult.Different
        if (value != other.value) return ComparisonResult.Different
        if (!ignoreDates) {
            if (fromStartDate != other.fromStartDate) return ComparisonResult.DifferentSensitive
            if (fromEndDate != other.fromEndDate) return ComparisonResult.DifferentSensitive
        }
        if (occurrenceCount != other.occurrenceCount) return ComparisonResult.DifferentSensitive
        if (!ignoreCycleMultiplier) {
            if (cycleMultiplier != other.cycleMultiplier) return ComparisonResult.DifferentSensitive
        }
        if (cycle != other.cycle) return ComparisonResult.DifferentSensitive
        if (enabled != other.enabled) return ComparisonResult.Different
        if (spent != other.spent) return ComparisonResult.Different
        if (targets != other.targets) return ComparisonResult.Different
        return ComparisonResult.Same
    }

    sealed class ComparisonResult {
        object Same : ComparisonResult()
        object Different : ComparisonResult()
        object DifferentSensitive : ComparisonResult()
    }

    override fun hashCode(): Int {
        var result = id ?: 0
        result = 31 * result + name.hashCode()
        result = 31 * result + (notes?.hashCode() ?: 0)
        result = 31 * result + type.hashCode()
        result = 31 * result + value.hashCode()
        result = 31 * result + fromStartDate.hashCode()
        result = 31 * result + fromEndDate.hashCode()
        result = 31 * result + (occurrenceCount ?: 0)
        result = 31 * result + cycleMultiplier
        result = 31 * result + cycle.hashCode()
        result = 31 * result + enabled.hashCode()
        result = 31 * result + (sourceData?.hashCode() ?: 0)
        result = 31 * result + (spent.hashCode())
        result = 31 * result + (targets?.hashCode() ?: 0)
        result = 31 * result + (savings?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "Spending(id=$id, name='$name', notes=$notes, type=$type, value=$value, fromStartDate=$fromStartDate, fromEndDate=$fromEndDate, occurrenceCount=$occurrenceCount, cycleMultiplier=$cycleMultiplier, cycle=$cycle, enabled=$enabled, sourceData=$sourceData, spent=$spent, targets=$targets, savings=$savings)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Spending

        if (id != other.id) return false
        if (name != other.name) return false
        if (notes != other.notes) return false
        if (type != other.type) return false
        if (value != other.value) return false
        if (fromStartDate != other.fromStartDate) return false
        if (fromEndDate != other.fromEndDate) return false
        if (occurrenceCount != other.occurrenceCount) return false
        if (cycleMultiplier != other.cycleMultiplier) return false
        if (cycle != other.cycle) return false
        if (enabled != other.enabled) return false
        if (sourceData != other.sourceData) return false
        if (spent != other.spent) return false
        if (targets != other.targets) return false
        if (!Arrays.equals(savings, other.savings)) return false
        if (target != other.target) return false

        return true
    }

    val isPersisted
        get() = id != null
}

operator fun Int.times(cycle: ApiSpending.Cycle) = Pair(this, cycle)

operator fun LocalDate.plus(cycle: Pair<Int, ApiSpending.Cycle>): LocalDate {
    return when (cycle.second) {
        DAYS -> this.plusDays(cycle.first.toLong())
        WEEKS -> this.plusWeeks(cycle.first.toLong())
        MONTHS -> this.plusMonths(cycle.first.toLong())
        YEARS -> this.plusYears(cycle.first.toLong())
    }
}

fun ApiSpending.Cycle.toMonths(): Double = when (this) {
    DAYS -> 0.0328731097961867
    WEEKS -> 0.2301368854194475
    MONTHS -> 1.0
    YEARS -> 12.0
}

/**
 * How many times the specified cycle fits between the specified start and end dates.
 */
operator fun Pair<LocalDate, LocalDate>.div(cycle: ApiSpending.Cycle): Float {
    return when (cycle) {
        DAYS -> (second.toEpochDay() - first.toEpochDay()).toFloat()
        WEEKS -> this / DAYS / 7f
        MONTHS -> second.monthsSinceYear0() - first.monthsSinceYear0()
        YEARS -> this / MONTHS / 12f
    }
}

private fun LocalDate.monthsSinceYear0() =
    this.year * 12 + (this.monthValue - 1) + this.dayOfMonth.toFloat() / 32

fun Pair<Pair<LocalDate, LocalDate>, Pair<LocalDate, LocalDate>>.overlap(cycle: ApiSpending.Cycle) =
    Pair(max(this.first.first, this.second.first),
            min(this.first.second, this.second.second)) / cycle

fun ApiSpending.Cycle.ordinal(date: LocalDate)
        : Int = when (this) {
    DAYS -> date.toEpochDay().toInt()
    WEEKS -> (date.with(DayOfWeek.MONDAY).toEpochDay() / 7).toInt()
    MONTHS -> date.year * 12 + date.monthValue
    YEARS -> date.year
}

fun LocalDate.firstCycleDay(cycle: ApiSpending.Cycle): LocalDate = when (cycle) {
    DAYS -> this
    WEEKS -> this.with(DayOfWeek.MONDAY)
    MONTHS -> this.withDayOfMonth(1)
    YEARS -> this.withMonth(1).withDayOfMonth(1)
}.atStartOfDay().plusDays(1).minusNanos(1).toLocalDate()

fun LocalDate.lastCycleDay(cycle: ApiSpending.Cycle): LocalDate = when (cycle) {
    DAYS -> this
    WEEKS -> this.with(DayOfWeek.SUNDAY)
    MONTHS -> this.with(lastDayOfMonth())
    YEARS -> this.with(lastDayOfYear())
}.atStartOfDay().plusDays(1).minusNanos(1).toLocalDate()

data class CycleSpending(
    val id: Int?,
    val spendingId: Int?,
    val from: LocalDate,
    val to: LocalDate,
    val amount: BigDecimal,
    val count: Int) {
    override fun toString() = "$from $to: $amount ($count)"
}