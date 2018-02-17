package com.gb.canibuythat.model

import com.gb.canibuythat.db.model.ApiSpending
import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalDate
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.temporal.ChronoUnit
import org.threeten.bp.temporal.TemporalAdjusters.lastDayOfMonth
import org.threeten.bp.temporal.TemporalAdjusters.lastDayOfYear
import java.util.*

class Spending(var id: Int? = null,
               var name: String,
               var notes: String? = null,
               var type: ApiSpending.Category,
               var value: Double,
               /**
                * Date before witch the transaction certainly won't happen. The repetition cycle
                * is added to this date.
                */
               var fromStartDate: Date,
               var fromEndDate: Date,
               var occurrenceCount: Int?,
               /**
                * For cycles like every 2 days or 2 weeks...
                */
               var cycleMultiplier: Int,
               /**
                * Does this modifier repeat every day/week/month/year. The first affected time-span
                * (specified by {@link Spending#fromStartDate} and {@link Spending#fromEndDate}) must not be larger
                * the this cycle.<br></br>
                * Ex: The first week of every month, cold months of the year, every weekend, every
                * semester
                */
               var cycle: ApiSpending.Cycle,
               var enabled: Boolean,
               var sourceData: MutableMap<String, String>?,
               var spent: Double?,
               var targets: Map<Date, Double>?,
               var savings: Array<Saving>?) {

    val target = targets?.maxBy { it.key }?.value

    val valuePerMonth: Double
        get() {
            return value / cycle.toMonth() * cycleMultiplier
        }

    fun compareForEditing(other: Any?, ignoreDates: Boolean, ignoreCycleMultiplier: Boolean): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Spending

        if (name != other.name) return false
        if (notes != other.notes) return false
        if (type != other.type) return false
        if (value != other.value) return false
        if (!ignoreDates) {
            if (fromStartDate != other.fromStartDate) return false
            if (fromEndDate != other.fromEndDate) return false
        }
        if (occurrenceCount != other.occurrenceCount) return false
        if (!ignoreCycleMultiplier) {
            if (cycleMultiplier != other.cycleMultiplier) return false
        }
        if (cycle != other.cycle) return false
        if (enabled != other.enabled) return false
        if (spent != other.spent) return false
        if (targets != other.targets) return false
        return true
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
        result = 31 * result + (spent?.hashCode() ?: 0)
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

fun Date.add(cycle: ApiSpending.Cycle, increment: Int): Date {
    val cal = Calendar.getInstance()
    cal.time = this
    when (cycle) {
        ApiSpending.Cycle.DAYS -> cal.add(Calendar.DAY_OF_MONTH, increment)
        ApiSpending.Cycle.WEEKS -> cal.add(Calendar.WEEK_OF_MONTH, increment)
        ApiSpending.Cycle.MONTHS -> cal.add(Calendar.MONTH, increment)
        ApiSpending.Cycle.YEARS -> cal.add(Calendar.YEAR, increment)
    }
    return cal.time
}

fun Calendar.add(cycle: ApiSpending.Cycle, increment: Int) {
    when (cycle) {
        ApiSpending.Cycle.DAYS -> this.add(Calendar.DAY_OF_MONTH, increment)
        ApiSpending.Cycle.WEEKS -> this.add(Calendar.WEEK_OF_MONTH, increment)
        ApiSpending.Cycle.MONTHS -> this.add(Calendar.MONTH, increment)
        ApiSpending.Cycle.YEARS -> this.add(Calendar.YEAR, increment)
    }
}

fun ApiSpending.Cycle.toMonth() = when (this) {
    ApiSpending.Cycle.DAYS -> 0.0328731097961867
    ApiSpending.Cycle.WEEKS -> 0.2301368854194475
    ApiSpending.Cycle.MONTHS -> 1.0
    ApiSpending.Cycle.YEARS -> 12.0
}

fun Pair<ZonedDateTime, ZonedDateTime>.span(cycle: ApiSpending.Cycle): Long {
    return when (cycle) {
        ApiSpending.Cycle.DAYS -> ChronoUnit.DAYS.between(this.first, this.second)
        ApiSpending.Cycle.WEEKS -> ChronoUnit.WEEKS.between(this.first, second)
        ApiSpending.Cycle.MONTHS -> ChronoUnit.MONTHS.between(this.first, second)
        ApiSpending.Cycle.YEARS -> ChronoUnit.YEARS.between(this.first, second)
    } + 1
}

fun ApiSpending.Cycle.of(date: LocalDate): Int {
    return when (this) {
        ApiSpending.Cycle.DAYS -> date.toEpochDay().toInt()
        ApiSpending.Cycle.WEEKS -> (date.with(DayOfWeek.MONDAY).toEpochDay() / 7).toInt()
        ApiSpending.Cycle.MONTHS -> date.monthValue
        ApiSpending.Cycle.YEARS -> date.year
    }
}

fun ZonedDateTime.lastCycleDay(cycle: ApiSpending.Cycle): LocalDate {
    return when (cycle) {
        ApiSpending.Cycle.DAYS -> this
        ApiSpending.Cycle.WEEKS -> this.with(DayOfWeek.SUNDAY)
        ApiSpending.Cycle.MONTHS -> this.with(lastDayOfMonth())
        ApiSpending.Cycle.YEARS -> this.with(lastDayOfYear())
    }.toLocalDate().atStartOfDay(this.zone).plusDays(1).minusNanos(1).toLocalDate()
}