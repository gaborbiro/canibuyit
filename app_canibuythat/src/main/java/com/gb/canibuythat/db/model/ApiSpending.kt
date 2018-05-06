package com.gb.canibuythat.db.model

import com.gb.canibuythat.R
import com.gb.canibuythat.db.Contract
import com.j256.ormlite.dao.ForeignCollection
import com.j256.ormlite.field.DataType
import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.field.ForeignCollectionField
import com.j256.ormlite.table.DatabaseTable
import java.time.LocalDate


/**
 * An income or expense that affects the balance of a specified time-span. The user
 * doesn't need to know the exact moment of payment.
 */
@DatabaseTable(tableName = Contract.Spending.TABLE)
class ApiSpending(
        @DatabaseField(generatedId = true, columnName = Contract.Spending._ID, canBeNull = false)
        var id: Int? = null,
        @DatabaseField(index = true, columnName = Contract.Spending.NAME, unique = true, canBeNull = false)
        var name: String? = null,
        @DatabaseField(columnName = Contract.Spending.NOTES, canBeNull = true)
        var notes: String? = null,
        @DatabaseField(columnName = Contract.Spending.TYPE, canBeNull = false)
        var type: Category? = null,
        @DatabaseField(columnName = Contract.Spending.VALUE, canBeNull = false)
        var value: Double? = null,
        @DatabaseField(columnName = Contract.Spending.FROM_START_DATE, canBeNull = false, dataType = DataType.SERIALIZABLE)
        var fromStartDate: LocalDate? = null,
        @DatabaseField(columnName = Contract.Spending.FROM_END_DATE, canBeNull = false, dataType = DataType.SERIALIZABLE)
        var fromEndDate: LocalDate? = null,
        @DatabaseField(columnName = Contract.Spending.OCCURRENCE_COUNT, canBeNull = true)
        var occurrenceCount: Int? = null,
        @DatabaseField(columnName = Contract.Spending.CYCLE_MULTIPLIER, canBeNull = false)
        var cycleMultiplier: Int? = null,
        @DatabaseField(columnName = Contract.Spending.CYCLE, canBeNull = false)
        var cycle: Cycle? = null,
        @DatabaseField(columnName = Contract.Spending.ENABLED, canBeNull = false)
        var enabled: Boolean? = null,
        @DatabaseField(columnName = Contract.Spending.SOURCE_DATA, canBeNull = true)
        var sourceData: String? = null,
        @DatabaseField(columnName = Contract.Spending.SPENT, canBeNull = true)
        var spent: Double? = null,
        @DatabaseField(columnName = Contract.Spending.TARGETS, canBeNull = true)
        var targets: String? = null,
        @ForeignCollectionField(eager = true)
        var savings: ForeignCollection<ApiSaving>? = null) {

    companion object {
        @JvmStatic
        val SOURCE_MONZO_CATEGORY: String = "monzo_category"
    }

    override fun toString(): String {
        return "ApiSpending(id=$id, name='$name', notes=$notes, type=$type, value=$value, fromStartDate=$fromStartDate, fromEndDate=$fromEndDate, occurrenceCount=$occurrenceCount, cycleMultiplier=$cycleMultiplier, cycle=$cycle, enabled=$enabled, sourceData=$sourceData, spent=$spent, target=$targets, savings=$savings)"
    }

    enum class Category(val defaultEnabled: Boolean = true) {
        ACCOMMODATION, AUTOMOBILE, CHILD_SUPPORT, DONATIONS_GIVEN, ENTERTAINMENT, FOOD,
        GIFTS_GIVEN, GROCERIES, HOUSEHOLD, INSURANCE, MEDICARE, PERSONAL_CARE, PETS,
        SELF_IMPROVEMENT, SPORTS_RECREATION, TAX, TRANSPORTATION, UTILITIES, VACATION,
        GIFTS_RECEIVED, INCOME(false), FINES,
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

        override fun toString(): String {
            return name.toLowerCase()
        }
    }
}