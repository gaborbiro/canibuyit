package com.gb.canibuythat.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.gb.canibuythat.R;
import com.gb.canibuythat.provider.Contract;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Calendar;
import java.util.Date;

/**
 * An income or expense that affects the balance of a specified time-span. The user
 * doesn't need to know the exact moment of payment.
 */
@DatabaseTable(tableName = Contract.BudgetItem.TABLE)
public class BudgetItem implements Parcelable {

    @DatabaseField(generatedId = true, columnName = Contract.BudgetItem._ID)
    public Integer id;
    @DatabaseField(index = true, columnName = Contract.BudgetItem.NAME, unique = true, canBeNull = false)
    public String name;
    @DatabaseField(columnName = Contract.BudgetItem.NOTES)
    public String notes;
    @DatabaseField(columnName = Contract.BudgetItem.TYPE, canBeNull = false)
    public BudgetItemType type;
    @DatabaseField(columnName = Contract.BudgetItem.AMOUNT, canBeNull = false)
    public Float amount;
    /**
     * Date before witch the transaction certainly won't happen. The repetition period
     * is added to this date.
     */
    @DatabaseField(columnName = Contract.BudgetItem.FIRST_OCCURRENCE_START, canBeNull = false)
    public Date firstOccurrenceStart;
    /**
     * Date by witch the transaction most certainly did happen. The repetition period is
     * added to this date.
     */
    @DatabaseField(columnName = Contract.BudgetItem.FIRST_OCCURRENCE_END, canBeNull = false)
    public Date firstOccurrenceEnd;
    /**
     * How many times this modifier will be spent/cashed in. If 0, the field
     * #periodMultiplier and #period are ignored
     */
    @DatabaseField(columnName = Contract.BudgetItem.OCCURRENCE_COUNT)
    public Integer occurrenceCount;
    /**
     * For periods like every 2 days or once every trimester...
     */
    @DatabaseField(columnName = Contract.BudgetItem.PERIOD_MULTIPLIER)
    public Integer periodMultiplier;
    /**
     * Does this modifier repeat every day/week/month/year. The first affected time-span
     * (specified by firstOccurrenceStart and firstOccurrenceEnd) must not be larger
     * the this period.<br>
     * Ex: The first week of every month, cold months of the year, every weekend, every
     * semester
     */
    @DatabaseField(columnName = Contract.BudgetItem.PERIOD_TYPE, canBeNull = false)
    public PeriodType periodType;

    @DatabaseField(columnName = Contract.BudgetItem.ENABLED, canBeNull = true)
    public boolean enabled = true;

    @DatabaseField(columnName = Contract.BudgetItem.ORDERING, canBeNull = true)
    public Integer ordering;

    public BudgetItem() {
    }

    private BudgetItem(Parcel in) {
        id = (Integer) in.readValue(Integer.class.getClassLoader());
        name = (String) in.readValue(String.class.getClassLoader());
        amount = (Float) in.readValue(Float.class.getClassLoader());
        notes = (String) in.readValue(String.class.getClassLoader());
        try {
            type = BudgetItemType.valueOf(((String) in.readValue(String.class.getClassLoader())).toUpperCase());
        } catch (IllegalArgumentException e) {
            // it means the original value was null
        }
        Long lowerDate = (Long) in.readValue(Long.class.getClassLoader());

        if (lowerDate != null) {
            firstOccurrenceStart = new Date(lowerDate);
        }
        Long upperDate = (Long) in.readValue(Long.class.getClassLoader());

        if (upperDate != null) {
            firstOccurrenceEnd = new Date(upperDate);
        }
        occurrenceCount = (Integer) in.readValue(Integer.class.getClassLoader());
        periodMultiplier = (Integer) in.readValue(Integer.class.getClassLoader());
        try {
            periodType = PeriodType.valueOf(((String) in.readValue(String.class.getClassLoader())).toUpperCase());
        } catch (IllegalArgumentException e) {
            // it means the original value was null
        }
        ordering = in.readInt();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BudgetItem that = (BudgetItem) o;

        if (enabled != that.enabled) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (notes != null ? !notes.equals(that.notes) : that.notes != null) return false;
        if (type != that.type) return false;
        if (amount != null ? !amount.equals(that.amount) : that.amount != null) return false;
        if (firstOccurrenceStart != null ? !firstOccurrenceStart.equals(that.firstOccurrenceStart) : that.firstOccurrenceStart != null)
            return false;
        if (firstOccurrenceEnd != null ? !firstOccurrenceEnd.equals(that.firstOccurrenceEnd) : that.firstOccurrenceEnd != null)
            return false;
        if (occurrenceCount != null ? !occurrenceCount.equals(that.occurrenceCount) : that.occurrenceCount != null)
            return false;
        if (periodMultiplier != null ? !periodMultiplier.equals(that.periodMultiplier) : that.periodMultiplier != null)
            return false;
        if (periodType != that.periodType) return false;
        return ordering != null ? ordering.equals(that.ordering) : that.ordering == null;
    }

    public boolean compareForEditing(Object o, boolean ignoreDates) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BudgetItem that = (BudgetItem) o;

        if (enabled != that.enabled) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (notes != null ? !notes.equals(that.notes) : that.notes != null)
            return false;
        if (type != that.type) return false;
        if (amount != null ? !amount.equals(that.amount) : that.amount != null)
            return false;
        if (!ignoreDates) {
            if (firstOccurrenceStart != null ? !firstOccurrenceStart.equals(
                    that.firstOccurrenceStart) : that.firstOccurrenceStart != null)
                return false;
            if (firstOccurrenceEnd != null ? !firstOccurrenceEnd.equals(
                    that.firstOccurrenceEnd) : that.firstOccurrenceEnd != null)
                return false;
        }
        if (occurrenceCount != null ? !occurrenceCount.equals(that.occurrenceCount)
                : that.occurrenceCount != null) return false;
        if (periodMultiplier != null ? !periodMultiplier.equals(that.periodMultiplier)
                : that.periodMultiplier != null) return false;
        return periodType == that.periodType;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (notes != null ? notes.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (amount != null ? amount.hashCode() : 0);
        result = 31 * result +
                (firstOccurrenceStart != null ? firstOccurrenceStart.hashCode() : 0);
        result = 31 * result +
                (firstOccurrenceEnd != null ? firstOccurrenceEnd.hashCode() : 0);
        result = 31 * result +
                (occurrenceCount != null ? occurrenceCount.hashCode() : 0);
        result = 31 * result +
                (periodMultiplier != null ? periodMultiplier.hashCode() : 0);
        result = 31 * result + (periodType != null ? periodType.hashCode() : 0);
        result = 31 * result + (enabled ? 1 : 0);
        result = 31 * result + (ordering != null ? ordering.hashCode() : 0);
        return result;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeValue(id);
        dest.writeValue(name);
        dest.writeValue(amount);
        dest.writeValue(notes);
        if (type != null) {
            dest.writeValue(type.name());
        } else {
            dest.writeValue(null);
        }
        if (firstOccurrenceStart != null) {
            dest.writeLong(firstOccurrenceStart.getTime());
        } else {
            dest.writeValue(null);
        }
        if (firstOccurrenceEnd != null) {
            dest.writeLong(firstOccurrenceEnd.getTime());
        } else {
            dest.writeValue(null);
        }
        dest.writeValue(occurrenceCount);
        dest.writeValue(periodMultiplier);
        if (periodType != null) {
            dest.writeValue(periodType.name());
        } else {
            dest.writeValue(null);
        }
        dest.writeInt(ordering);
    }

    public static final Parcelable.Creator<BudgetItem> CREATOR =
            new Parcelable.Creator<BudgetItem>() {
                @Override
                public BudgetItem createFromParcel(Parcel in) {
                    return new BudgetItem(in);
                }

                @Override
                public BudgetItem[] newArray(int size) {
                    return new BudgetItem[size];
                }
            };

    public boolean isPersisted() {
        return id != null;
    }

    // money comes in
    public static final int BUDGET_ITEM_TYPE_IN = 1;
    // money goes out
    public static final int BUDGET_ITEM_TYPE_OUT = -1;

    public enum BudgetItemType {
        ACCOMMODATION, AUTOMOBILE, CHILD_SUPPORT, DONATIONS_GIVEN, ENTERTAINMENT, FOOD,
        GIFTS_GIVEN, GROCERIES, HOUSEHOLD, INSURANCE, MEDICARE, PERSONAL_CARE, PETS,
        SELF_IMPROVEMENT, SPORTS_RECREATION, TAX, TRANSPORTATION, UTILITIES, VACATION,
        GIFTS_RECEIVED(BUDGET_ITEM_TYPE_IN), INCOME(BUDGET_ITEM_TYPE_IN), FINES,
        ONLINE_SERVICES, LUXURY, CASH, SAVINGS,
        OTHER;

        private final byte sign;

        BudgetItemType() {
            this(BUDGET_ITEM_TYPE_OUT);
        }

        BudgetItemType(int sign) {
            this.sign = (byte) sign;
        }

        @Override
        public String toString() {
            return name().toLowerCase();
        }

        public byte getSign() {
            return sign;
        }
    }

    public enum PeriodType {
        DAYS(R.plurals.days), WEEKS(R.plurals.weeks), MONTHS(R.plurals.months),
        YEARS(R.plurals.years);

        public final int strRes;

        PeriodType(int strRes) {
            this.strRes = strRes;
        }

        public void apply(Calendar c, int increment) {
            switch (this) {
                case DAYS:
                    c.add(Calendar.DAY_OF_MONTH, increment);
                    break;
                case WEEKS:
                    c.add(Calendar.WEEK_OF_MONTH, increment);
                    break;
                case MONTHS:
                    c.add(Calendar.MONTH, increment);
                    break;
                case YEARS:
                    c.add(Calendar.YEAR, increment);
                    break;
            }
        }

        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }
}