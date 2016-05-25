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
@DatabaseTable(tableName = Contract.BudgetItem.TABLE) public class BudgetItem
        implements Parcelable {

    public static final Parcelable.Creator<BudgetItem> CREATOR =
            new Parcelable.Creator<BudgetItem>() {

                @Override public BudgetItem createFromParcel(Parcel in) {
                    return new BudgetItem(in);
                }

                @Override public BudgetItem[] newArray(int size) {
                    return new BudgetItem[size];
                }
            };
    @DatabaseField(generatedId = true, columnName = Contract.BudgetItem._ID)
    public Integer mId;
    @DatabaseField(index = true, columnName = Contract.BudgetItem.NAME, unique = true,
            canBeNull = false) public String mName;
    @DatabaseField(columnName = Contract.BudgetItem.NOTES) public String mNotes;
    @DatabaseField(columnName = Contract.BudgetItem.TYPE, canBeNull = false)
    public BudgetItemType mType;
    @DatabaseField(columnName = Contract.BudgetItem.AMOUNT, canBeNull = false)
    public Float mAmount;
    /**
     * Date before witch the transaction certainly won't happen. The repetition period
     * is added to this date.
     */
    @DatabaseField(columnName = Contract.BudgetItem.FIRST_OCCURRENCE_START,
            canBeNull = false) public Date mFirstOccurrenceStart;
    /**
     * Date by witch the transaction most certainly did happen. The repetition period is
     * added to this date.
     */
    @DatabaseField(columnName = Contract.BudgetItem.FIRST_OCCURRENCE_END,
            canBeNull = false) public Date mFirstOccurrenceEnd;
    /**
     * How many times this modifier will be spent/cashed in. If 0, the field
     * #periodMultiplier and #period are ignored
     */
    @DatabaseField(columnName = Contract.BudgetItem.OCCURRENCE_COUNT) public Integer
            mOccurrenceCount;
    /**
     * For periods like every 2 days or once every trimester...
     */
    @DatabaseField(columnName = Contract.BudgetItem.PERIOD_MULTIPLIER) public Integer
            mPeriodMultiplier;
    /**
     * Does this modifier repeat every day/week/month/year. The first affected time-span
     * (specified by mFirstOccurrenceStart and mFirstOccurrenceEnd) must not be larger
     * the this period.<br>
     * Ex: The first week of every month, cold months of the year, every weekend, every
     * semester
     */
    @DatabaseField(columnName = Contract.BudgetItem.PERIOD_TYPE, canBeNull = false)
    public PeriodType mPeriodType;

    @DatabaseField(columnName = Contract.BudgetItem.ENABLED, canBeNull = true)
    public boolean mEnabled = true;

    public BudgetItem() {
    }

    private BudgetItem(Parcel in) {
        mId = (Integer) in.readValue(Integer.class.getClassLoader());
        mName = (String) in.readValue(String.class.getClassLoader());
        mAmount = (Float) in.readValue(Float.class.getClassLoader());
        mNotes = (String) in.readValue(String.class.getClassLoader());
        try {
            mType = BudgetItemType.valueOf(
                    ((String) in.readValue(String.class.getClassLoader())).toUpperCase());
        } catch (IllegalArgumentException e) {
            // it means the original value was null
        }
        Long lowerDate = (Long) in.readValue(Long.class.getClassLoader());

        if (lowerDate != null) {
            mFirstOccurrenceStart = new Date(lowerDate);
        }
        Long upperDate = (Long) in.readValue(Long.class.getClassLoader());

        if (upperDate != null) {
            mFirstOccurrenceEnd = new Date(upperDate);
        }
        mOccurrenceCount = (Integer) in.readValue(Integer.class.getClassLoader());
        mPeriodMultiplier = (Integer) in.readValue(Integer.class.getClassLoader());
        try {
            mPeriodType = PeriodType.valueOf((String) ((String) in.readValue(
                    String.class.getClassLoader())).toUpperCase());
        } catch (IllegalArgumentException e) {
            // it means the original value was null
        }
    }

    public boolean compareForEditing(Object o, boolean ignoreDates) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BudgetItem that = (BudgetItem) o;

        if (mEnabled != that.mEnabled) return false;
        if (mName != null ? !mName.equals(that.mName) : that.mName != null) return false;
        if (mNotes != null ? !mNotes.equals(that.mNotes) : that.mNotes != null)
            return false;
        if (mType != that.mType) return false;
        if (mAmount != null ? !mAmount.equals(that.mAmount) : that.mAmount != null)
            return false;
        if (!ignoreDates) {
            if (mFirstOccurrenceStart != null ? !mFirstOccurrenceStart.equals(
                    that.mFirstOccurrenceStart) : that.mFirstOccurrenceStart != null)
                return false;
            if (mFirstOccurrenceEnd != null ? !mFirstOccurrenceEnd.equals(
                    that.mFirstOccurrenceEnd) : that.mFirstOccurrenceEnd != null)
                return false;
        }
        if (mOccurrenceCount != null ? !mOccurrenceCount.equals(that.mOccurrenceCount)
                                     : that.mOccurrenceCount != null) return false;
        if (mPeriodMultiplier != null ? !mPeriodMultiplier.equals(that.mPeriodMultiplier)
                                      : that.mPeriodMultiplier != null) return false;
        return mPeriodType == that.mPeriodType;
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BudgetItem that = (BudgetItem) o;

        if (mEnabled != that.mEnabled) return false;
        if (mId != null ? !mId.equals(that.mId) : that.mId != null) return false;
        if (mName != null ? !mName.equals(that.mName) : that.mName != null) return false;
        if (mNotes != null ? !mNotes.equals(that.mNotes) : that.mNotes != null)
            return false;
        if (mType != that.mType) return false;
        if (mAmount != null ? !mAmount.equals(that.mAmount) : that.mAmount != null)
            return false;
        if (mFirstOccurrenceStart != null ? !mFirstOccurrenceStart.equals(
                that.mFirstOccurrenceStart) : that.mFirstOccurrenceStart != null)
            return false;
        if (mFirstOccurrenceEnd != null ? !mFirstOccurrenceEnd.equals(
                that.mFirstOccurrenceEnd) : that.mFirstOccurrenceEnd != null)
            return false;
        if (mOccurrenceCount != null ? !mOccurrenceCount.equals(that.mOccurrenceCount)
                                     : that.mOccurrenceCount != null) return false;
        if (mPeriodMultiplier != null ? !mPeriodMultiplier.equals(that.mPeriodMultiplier)
                                      : that.mPeriodMultiplier != null) return false;
        return mPeriodType == that.mPeriodType;

    }

    @Override public int hashCode() {
        int result = 0;
        result = 31 * result + (mName != null ? mName.hashCode() : 0);
        result = 31 * result + (mNotes != null ? mNotes.hashCode() : 0);
        result = 31 * result + (mType != null ? mType.hashCode() : 0);
        result = 31 * result + (mAmount != null ? mAmount.hashCode() : 0);
        result = 31 * result +
                (mFirstOccurrenceStart != null ? mFirstOccurrenceStart.hashCode() : 0);
        result = 31 * result +
                (mFirstOccurrenceEnd != null ? mFirstOccurrenceEnd.hashCode() : 0);
        result = 31 * result +
                (mOccurrenceCount != null ? mOccurrenceCount.hashCode() : 0);
        result = 31 * result +
                (mPeriodMultiplier != null ? mPeriodMultiplier.hashCode() : 0);
        result = 31 * result + (mPeriodType != null ? mPeriodType.hashCode() : 0);
        result = 31 * result + (mEnabled ? 1 : 0);
        return result;
    }

    @Override public int describeContents() {
        return 0;
    }

    @Override public void writeToParcel(Parcel dest, int flags) {
        dest.writeValue(mId);
        dest.writeValue(mName);
        dest.writeValue(mAmount);
        dest.writeValue(mNotes);
        if (mType != null) {
            dest.writeValue(mType.name());
        } else {
            dest.writeValue(null);
        }
        if (mFirstOccurrenceStart != null) {
            dest.writeLong(mFirstOccurrenceStart.getTime());
        } else {
            dest.writeValue(null);
        }
        if (mFirstOccurrenceEnd != null) {
            dest.writeLong(mFirstOccurrenceEnd.getTime());
        } else {
            dest.writeValue(null);
        }
        dest.writeValue(mOccurrenceCount);
        dest.writeValue(mPeriodMultiplier);
        if (mPeriodType != null) {
            dest.writeValue(mPeriodType.name());
        } else {
            dest.writeValue(null);
        }
    }

    public boolean isPersisted() {
        return mId != null;
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

        @Override public String toString() {
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

        @Override public String toString() {
            return name().toLowerCase();
        }
    }
}