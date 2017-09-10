package com.gb.canibuythat.model

import com.gb.canibuythat.R
import com.gb.canibuythat.db.Contract
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
        @JvmStatic val SOURCE_MONZO_CATEGORY: String = "monzo_category"
    }

    @DatabaseField(generatedId = true, columnName = Contract.Spending._ID, canBeNull = true)
    var id: Int? = null

    @DatabaseField(index = true, columnName = Contract.Spending.NAME, unique = true, canBeNull = false)
    var name: String? = null

    @DatabaseField(columnName = Contract.Spending.NOTES, canBeNull = true)
    var notes: String? = null

    @DatabaseField(columnName = Contract.Spending.TYPE, canBeNull = false)
    var type: Category? = null

    @DatabaseField(columnName = Contract.Spending.VALUE, canBeNull = false)
    var value: Double = 0.0

    /**
     * Date before witch the transaction certainly won't happen. The repetition cycle
     * is added to this date.
     */
    @DatabaseField(columnName = Contract.Spending.FROM_START_DATE, canBeNull = false)
    lateinit var fromStartDate: Date

    /**
     * Date by witch the transaction most certainly did happen. The repetition cycle is
     * added to this date.
     */
    @DatabaseField(columnName = Contract.Spending.FROM_END_DATE, canBeNull = false)
    lateinit var fromEndDate: Date

    /**
     * How many times this modifier will be spent/cashed in. If 0, the field
     * #cycleMultiplier and #cycle are ignored
     */
    @DatabaseField(columnName = Contract.Spending.OCCURRENCE_COUNT, canBeNull = true)
    var occurrenceCount: Int? = null

    /**
     * For cycles like every 2 days or 2 weeks...
     */
    @DatabaseField(columnName = Contract.Spending.CYCLE_MULTIPLIER, canBeNull = true)
    var cycleMultiplier: Int? = null

    /**
     * Does this modifier repeat every day/week/month/year. The first affected time-span
     * (specified by {@link Spending#fromStartDate} and {@link Spending#fromEndDate}) must not be larger
     * the this cycle.<br></br>
     * Ex: The first week of every month, cold months of the year, every weekend, every
     * semester
     */
    @DatabaseField(columnName = Contract.Spending.CYCLE, canBeNull = false)
    var cycle: Cycle? = null

    @DatabaseField(columnName = Contract.Spending.ENABLED, canBeNull = true)
    var enabled = true

    @DatabaseField(columnName = Contract.Spending.SOURCE_DATA, dataType = DataType.SERIALIZABLE, canBeNull = false)
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
        if (value != other.value) return false
        if (fromStartDate != other.fromStartDate) return false
        if (fromEndDate != other.fromEndDate) return false
        if (occurrenceCount != other.occurrenceCount) return false
        if (cycleMultiplier != other.cycleMultiplier) return false
        if (cycle != other.cycle) return false
        if (enabled != other.enabled) return false
        if (sourceData != other.sourceData) return false
        if (spent != other.spent) return false
        if (target != other.target) return false

        return true
    }

    fun compareForEditing(other: Any?, ignoreDates: Boolean, ignoreCycleMultiplier: Boolean): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as Spending

        if (id != other.id) return false
        if (enabled != other.enabled) return false
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
        if (sourceData != other.sourceData) return false
        if (spent != other.spent) return false
        if (target != other.target) return false
        return true
    }

    override fun hashCode(): Int {
        var result = id ?: 0
        result = 31 * result + (name?.hashCode() ?: 0)
        result = 31 * result + (notes?.hashCode() ?: 0)
        result = 31 * result + (type?.hashCode() ?: 0)
        result = 31 * result + (value.hashCode())
        result = 31 * result + (fromStartDate.hashCode())
        result = 31 * result + (fromEndDate.hashCode())
        result = 31 * result + (occurrenceCount ?: 0)
        result = 31 * result + (cycleMultiplier ?: 0)
        result = 31 * result + (cycle?.hashCode() ?: 0)
        result = 31 * result + enabled.hashCode()
        result = 31 * result + (sourceData.hashCode())
        result = 31 * result + (spent?.hashCode() ?: 0)
        result = 31 * result + (target?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "Spending(id=$id, name=$name, notes=$notes, type=$type, value=$value, fromStartDate=$fromStartDate, fromEndDate=$fromEndDate, occurrenceCount=$occurrenceCount, cycleMultiplier=$cycleMultiplier, cycle=$cycle, enabled=$enabled, sourceData=$sourceData, spent=$spent, target=$target)"
    }


    val isPersisted: Boolean
        get() = id != null

    enum class Category(val defaultEnabled: Boolean = true) {
        ACCOMMODATION, AUTOMOBILE, CHILD_SUPPORT, DONATIONS_GIVEN, ENTERTAINMENT, FOOD,
        GIFTS_GIVEN, GROCERIES, HOUSEHOLD, INSURANCE, MEDICARE, PERSONAL_CARE, PETS,
        SELF_IMPROVEMENT, SPORTS_RECREATION, TAX, TRANSPORTATION, UTILITIES,  VACATION(false),
        GIFTS_RECEIVED, INCOME, FINES,
        ONLINE_SERVICES, LUXURY, CASH, SAVINGS, EXPENSES(false), OTHER;

        override fun toString(): String {
            return name.toLowerCase()
        }
    }

    enum class Cycle(val strRes: Int) {
        DAYS(R.plurals.day),
        WEEKS(R.plurals.week),
        MONTHS(R.plurals.month),
        YEARS(R.plurals.year);

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