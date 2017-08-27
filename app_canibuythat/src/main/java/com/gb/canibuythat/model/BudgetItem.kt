package com.gb.canibuythat.model

import com.gb.canibuythat.R
import com.gb.canibuythat.provider.Contract
import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.table.DatabaseTable
import java.util.*

/**
 * An income or expense that affects the balance of a specified time-span. The user
 * doesn't need to know the exact moment of payment.
 */
@DatabaseTable(tableName = Contract.BudgetItem.TABLE)
class BudgetItem {

    @DatabaseField(generatedId = true, columnName = Contract.BudgetItem._ID)
    var id: Int? = null

    @DatabaseField(index = true, columnName = Contract.BudgetItem.NAME, unique = true, canBeNull = false)
    var name: String? = null

    @DatabaseField(columnName = Contract.BudgetItem.NOTES)
    var notes: String? = null

    @DatabaseField(columnName = Contract.BudgetItem.TYPE, canBeNull = false)
    var type: BudgetItemType? = null

    @DatabaseField(columnName = Contract.BudgetItem.AMOUNT, canBeNull = false)
    var amount: Double? = null

    /**
     * Date before witch the transaction certainly won't happen. The repetition period
     * is added to this date.
     */
    @DatabaseField(columnName = Contract.BudgetItem.FIRST_OCCURRENCE_START, canBeNull = false)
    var firstOccurrenceStart: Date? = null

    /**
     * Date by witch the transaction most certainly did happen. The repetition period is
     * added to this date.
     */
    @DatabaseField(columnName = Contract.BudgetItem.FIRST_OCCURRENCE_END, canBeNull = false)
    var firstOccurrenceEnd: Date? = null

    /**
     * How many times this modifier will be spent/cashed in. If 0, the field
     * #periodMultiplier and #period are ignored
     */
    @DatabaseField(columnName = Contract.BudgetItem.OCCURRENCE_COUNT)
    var occurrenceCount: Int? = null

    /**
     * For periods like every 2 days or once every trimester...
     */
    @DatabaseField(columnName = Contract.BudgetItem.PERIOD_MULTIPLIER)
    var periodMultiplier: Int? = null

    /**
     * Does this modifier repeat every day/week/month/year. The first affected time-span
     * (specified by firstOccurrenceStart and firstOccurrenceEnd) must not be larger
     * the this period.<br></br>
     * Ex: The first week of every month, cold months of the year, every weekend, every
     * semester
     */
    @DatabaseField(columnName = Contract.BudgetItem.PERIOD_TYPE, canBeNull = false)
    var periodType: PeriodType? = null

    @DatabaseField(columnName = Contract.BudgetItem.ENABLED, canBeNull = true)
    var enabled = true

    @DatabaseField(columnName = Contract.BudgetItem.ORDERING, canBeNull = true)
    var ordering: Int? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false

        val that = other as BudgetItem?

        if (enabled != that!!.enabled) return false
        if (if (id != null) id != that.id else that.id != null) return false
        if (if (name != null) name != that.name else that.name != null) return false
        if (if (notes != null) notes != that.notes else that.notes != null) return false
        if (type != that.type) return false
        if (if (amount != null) amount != that.amount else that.amount != null) return false
        if (if (firstOccurrenceStart != null) firstOccurrenceStart != that.firstOccurrenceStart else that.firstOccurrenceStart != null)
            return false
        if (if (firstOccurrenceEnd != null) firstOccurrenceEnd != that.firstOccurrenceEnd else that.firstOccurrenceEnd != null)
            return false
        if (if (occurrenceCount != null) occurrenceCount != that.occurrenceCount else that.occurrenceCount != null)
            return false
        if (if (periodMultiplier != null) periodMultiplier != that.periodMultiplier else that.periodMultiplier != null)
            return false
        if (periodType != that.periodType) return false
        return if (ordering != null) ordering == that.ordering else that.ordering == null
    }

    fun compareForEditing(o: Any?, ignoreDates: Boolean): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false

        val that = o as BudgetItem?

        if (enabled != that!!.enabled) return false
        if (if (name != null) name != that.name else that.name != null) return false
        if (if (notes != null) notes != that.notes else that.notes != null)
            return false
        if (type != that.type) return false
        if (if (amount != null) amount != that.amount else that.amount != null)
            return false
        if (!ignoreDates) {
            if (if (firstOccurrenceStart != null)
                firstOccurrenceStart != that.firstOccurrenceStart
            else
                that.firstOccurrenceStart != null)
                return false
            if (if (firstOccurrenceEnd != null)
                firstOccurrenceEnd != that.firstOccurrenceEnd
            else
                that.firstOccurrenceEnd != null)
                return false
        }
        if (if (occurrenceCount != null)
            occurrenceCount != that.occurrenceCount
        else
            that.occurrenceCount != null)
            return false
        if (if (periodMultiplier != null)
            periodMultiplier != that.periodMultiplier
        else
            that.periodMultiplier != null)
            return false
        return periodType == that.periodType
    }

    override fun hashCode(): Int {
        var result = if (id != null) id!!.hashCode() else 0
        result = 31 * result + if (name != null) name!!.hashCode() else 0
        result = 31 * result + if (notes != null) notes!!.hashCode() else 0
        result = 31 * result + if (type != null) type!!.hashCode() else 0
        result = 31 * result + if (amount != null) amount!!.hashCode() else 0
        result = 31 * result + if (firstOccurrenceStart != null) firstOccurrenceStart!!.hashCode() else 0
        result = 31 * result + if (firstOccurrenceEnd != null) firstOccurrenceEnd!!.hashCode() else 0
        result = 31 * result + if (occurrenceCount != null) occurrenceCount!!.hashCode() else 0
        result = 31 * result + if (periodMultiplier != null) periodMultiplier!!.hashCode() else 0
        result = 31 * result + if (periodType != null) periodType!!.hashCode() else 0
        result = 31 * result + if (enabled) 1 else 0
        result = 31 * result + if (ordering != null) ordering!!.hashCode() else 0
        return result
    }

    val isPersisted: Boolean
        get() = id != null

    enum class BudgetItemType(val defaultEnabled: Boolean = true) {
        ACCOMMODATION, AUTOMOBILE, CHILD_SUPPORT, DONATIONS_GIVEN, ENTERTAINMENT, FOOD,
        GIFTS_GIVEN, GROCERIES, HOUSEHOLD, INSURANCE, MEDICARE, PERSONAL_CARE, PETS,
        SELF_IMPROVEMENT, SPORTS_RECREATION, TAX, TRANSPORTATION, UTILITIES, VACATION(false),
        GIFTS_RECEIVED, INCOME, FINES,
        ONLINE_SERVICES, LUXURY, CASH, SAVINGS, EXPENSES(false), OTHER;

        override fun toString(): String {
            return name.toLowerCase()
        }
    }

    enum class PeriodType(val strRes: Int) {
        DAYS(R.plurals.days),
        WEEKS(R.plurals.weeks),
        MONTHS(R.plurals.months),
        YEARS(R.plurals.years);

        fun apply(c: Calendar, increment: Int) {
            when (this) {
                DAYS -> c.add(Calendar.DAY_OF_MONTH, increment)
                WEEKS -> c.add(Calendar.WEEK_OF_MONTH, increment)
                MONTHS -> c.add(Calendar.MONTH, increment)
                YEARS -> c.add(Calendar.YEAR, increment)
            }
        }

        override fun toString(): String {
            return name.toLowerCase()
        }
    }
}