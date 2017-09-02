package com.gb.canibuythat.model

import com.gb.canibuythat.R
import com.gb.canibuythat.provider.Contract
import com.j256.ormlite.field.DataType
import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.table.DatabaseTable
import java.util.*


/**
 * An income or expense that affects the balance of a specified time-span. The user
 * doesn't need to know the exact moment of payment.
 */
@DatabaseTable(tableName = Contract.Spending.TABLE)
class Spending {

    companion object {
        val SOURCE_MONZO_CATEGORY: String = "monzo_category"
    }

    @DatabaseField(generatedId = true, columnName = Contract.Spending._ID)
    var id: Int? = null

    @DatabaseField(index = true, columnName = Contract.Spending.NAME, unique = true, canBeNull = false)
    var name: String? = null

    @DatabaseField(columnName = Contract.Spending.NOTES)
    var notes: String? = null

    @DatabaseField(columnName = Contract.Spending.TYPE, canBeNull = false)
    var type: Category? = null

    @DatabaseField(columnName = Contract.Spending.SPENDING, canBeNull = false)
    var average: Double? = null

    /**
     * Date before witch the transaction certainly won't happen. The repetition period
     * is added to this date.
     */
    @DatabaseField(columnName = Contract.Spending.FIRST_OCCURRENCE_START, canBeNull = false)
    var firstOccurrenceStart: Date? = null

    /**
     * Date by witch the transaction most certainly did happen. The repetition period is
     * added to this date.
     */
    @DatabaseField(columnName = Contract.Spending.FIRST_OCCURRENCE_END, canBeNull = false)
    var firstOccurrenceEnd: Date? = null

    /**
     * How many times this modifier will be spent/cashed in. If 0, the field
     * #periodMultiplier and #period are ignored
     */
    @DatabaseField(columnName = Contract.Spending.OCCURRENCE_COUNT)
    var occurrenceCount: Int? = null

    /**
     * For periods like every 2 days or once every trimester...
     */
    @DatabaseField(columnName = Contract.Spending.PERIOD_MULTIPLIER)
    var periodMultiplier: Int? = null

    /**
     * Does this modifier repeat every day/week/month/year. The first affected time-span
     * (specified by firstOccurrenceStart and firstOccurrenceEnd) must not be larger
     * the this period.<br></br>
     * Ex: The first week of every month, cold months of the year, every weekend, every
     * semester
     */
    @DatabaseField(columnName = Contract.Spending.PERIOD_TYPE, canBeNull = false)
    var period: Period? = null

    @DatabaseField(columnName = Contract.Spending.ENABLED, canBeNull = true)
    var enabled = true

    @DatabaseField(columnName = Contract.Spending.SOURCE_DATA, canBeNull = false, dataType = DataType.SERIALIZABLE)
    val sourceData: SerializableMap<String, String> = SerializableMap()

    @DatabaseField(columnName = Contract.Spending.SPENT, canBeNull = true)
    var spent: Double? = null

    @DatabaseField(columnName = Contract.Spending.TARGET, canBeNull = true)
    var target: Double? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as Spending

        if (id != other.id) return false
        if (name != other.name) return false
        if (notes != other.notes) return false
        if (type != other.type) return false
        if (average != other.average) return false
        if (firstOccurrenceStart != other.firstOccurrenceStart) return false
        if (firstOccurrenceEnd != other.firstOccurrenceEnd) return false
        if (occurrenceCount != other.occurrenceCount) return false
        if (periodMultiplier != other.periodMultiplier) return false
        if (period != other.period) return false
        if (enabled != other.enabled) return false
        if (!sourceData.equals(other.sourceData)) return false
        if (spent != other.spent) return false
        if (target != other.target) return false

        return true
    }

    fun compareForEditing(other: Any?, ignoreDates: Boolean): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as Spending

        if (id != other.id) return false
        if (enabled != other.enabled) return false
        if (name != other.name) return false
        if (notes != other.notes) return false
        if (type != other.type) return false
        if (average != other.average) return false
        if (!ignoreDates) {
            if (firstOccurrenceStart != other.firstOccurrenceStart) return false
            if (firstOccurrenceEnd != other.firstOccurrenceEnd) return false
        }
        if (occurrenceCount != other.occurrenceCount) return false
        if (periodMultiplier != other.periodMultiplier) return false
        if (period != other.period) return false
        if (enabled != other.enabled) return false
        if (!sourceData.equals(other.sourceData)) return false
        if (spent != other.spent) return false
        if (target != other.target) return false
        return true
    }

    override fun hashCode(): Int {
        var result = id ?: 0
        result = 31 * result + (name?.hashCode() ?: 0)
        result = 31 * result + (notes?.hashCode() ?: 0)
        result = 31 * result + (type?.hashCode() ?: 0)
        result = 31 * result + (average?.hashCode() ?: 0)
        result = 31 * result + (firstOccurrenceStart?.hashCode() ?: 0)
        result = 31 * result + (firstOccurrenceEnd?.hashCode() ?: 0)
        result = 31 * result + (occurrenceCount ?: 0)
        result = 31 * result + (periodMultiplier ?: 0)
        result = 31 * result + (period?.hashCode() ?: 0)
        result = 31 * result + enabled.hashCode()
        result = 31 * result + (sourceData.hashCode())
        result = 31 * result + (spent?.hashCode() ?: 0)
        result = 31 * result + (target?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "Spending(id=$id, name=$name, notes=$notes, type=$type, average=$average, firstOccurrenceStart=$firstOccurrenceStart, firstOccurrenceEnd=$firstOccurrenceEnd, occurrenceCount=$occurrenceCount, periodMultiplier=$periodMultiplier, period=$period, enabled=$enabled, sourceData=$sourceData, spent=$spent, target=$target)"
    }


    val isPersisted: Boolean
        get() = id != null

    enum class Category(val defaultEnabled: Boolean = true) {
        ACCOMMODATION, AUTOMOBILE, CHILD_SUPPORT, DONATIONS_GIVEN, ENTERTAINMENT, FOOD,
        GIFTS_GIVEN, GROCERIES, HOUSEHOLD, INSURANCE, MEDICARE, PERSONAL_CARE, PETS,
        SELF_IMPROVEMENT, SPORTS_RECREATION, TAX, TRANSPORTATION, UTILITIES, VACATION(false),
        GIFTS_RECEIVED, INCOME, FINES,
        ONLINE_SERVICES, LUXURY, CASH, SAVINGS, EXPENSES(false), OTHER;

        override fun toString(): String {
            return name.toLowerCase()
        }
    }

    enum class Period(val strRes: Int) {
        DAYS(R.string.daily),
        WEEKS(R.string.weekly),
        MONTHS(R.string.monthly),
        YEARS(R.string.yearly);

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