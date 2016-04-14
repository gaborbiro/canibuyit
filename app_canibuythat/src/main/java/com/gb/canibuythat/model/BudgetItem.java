package com.gb.canibuythat.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.gb.canibuythat.provider.Contract;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Date;

/**
 * An income or expense that affects the balance of a specified time-span. The user
 * doesn't need to know the exact moment of payment.
 */
@DatabaseTable(tableName = Contract.BudgetItem.TABLE)
public class BudgetItem implements Parcelable {

    // money goes out
    private static final int OUT = -1;

    // money comes in
    private static final int IN = 1;

    public enum BudgetItemType {
        ACCOMMODATION, AUTOMOBILE, CHILD_SUPPORT, DONATIONS_GIVEN, ENTERTAINMENT, FOOD,
        GIFTS_GIVEN, GROCERIES, HOUSEHOLD, INSURANCE, MEDICARE, PERSONAL_CARE, PETS,
        SELF_IMPROVEMENT, SPORTS_RECREATION, TAX, TRANSPORTATION, UTILITIES, VACATION,
        GIFTS_RECEIVED(IN), INCOME(IN), FINES, ONLINE_SERVICES, LUXURY, CASH, SAVINGS,
        OTHER;

        private final byte sign;

        BudgetItemType() {
            this(OUT);
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
        DAYS, WEEKS, MONTHS, YEARS;

        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }

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
            mOccurenceCount;

    /**
     * For periods like every 2 days or once every trimester...
     */
    @DatabaseField(columnName = Contract.BudgetItem.PERIOD_MULTIPLIER) public Integer
            mPeriodMultiplier;

    /**
     * Does this modifier repeat every day/week/month/year. The first affected time-span
     * (specified by
     * firstOccurenceStart and firstOccurenceEnd) must not be larger the this period.<br>
     * Ex: The first week of every month, cold months of the year, every weekend, every
     * semester
     */
    @DatabaseField(columnName = Contract.BudgetItem.PERIOD_TYPE, canBeNull = false)
    public PeriodType mPeriodType;

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
        mOccurenceCount = (Integer) in.readValue(Integer.class.getClassLoader());
        mPeriodMultiplier = (Integer) in.readValue(Integer.class.getClassLoader());
        try {
            mPeriodType = PeriodType.valueOf((String) ((String) in.readValue(
                    String.class.getClassLoader())).toUpperCase());
        } catch (IllegalArgumentException e) {
            // it means the original value was null
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BudgetItem that = (BudgetItem) o;

        if (mAmount != null ? !mAmount.equals(that.mAmount) : that.mAmount != null)
            return false;
        if (mFirstOccurrenceStart != null ? !mFirstOccurrenceStart.equals(
                that.mFirstOccurrenceStart) : that.mFirstOccurrenceStart != null)
            return false;
        if (mNotes != null ? !mNotes.equals(that.mNotes) : that.mNotes != null)
            return false;
        if (mPeriodMultiplier != null ? !mPeriodMultiplier.equals(that.mPeriodMultiplier)
                                      : that.mPeriodMultiplier != null) return false;
        if (mPeriodType != that.mPeriodType) return false;
        if (mOccurenceCount != null ? !mOccurenceCount.equals(that.mOccurenceCount)
                                    : that.mOccurenceCount != null) return false;
        if (mName != null ? !mName.equals(that.mName) : that.mName != null) return false;
        if (mType != that.mType) return false;
        if (mFirstOccurrenceEnd != null ? !mFirstOccurrenceEnd.equals(
                that.mFirstOccurrenceEnd) : that.mFirstOccurrenceEnd != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = mName != null ? mName.hashCode() : 0;
        result = 31 * result + (mNotes != null ? mNotes.hashCode() : 0);
        result = 31 * result + (mType != null ? mType.hashCode() : 0);
        result = 31 * result + (mAmount != null ? mAmount.hashCode() : 0);
        result = 31 * result +
                (mFirstOccurrenceStart != null ? mFirstOccurrenceStart.hashCode() : 0);
        result = 31 * result +
                (mFirstOccurrenceEnd != null ? mFirstOccurrenceEnd.hashCode() : 0);
        result = 31 * result + (mOccurenceCount != null ? mOccurenceCount.hashCode() : 0);
        result = 31 * result +
                (mPeriodMultiplier != null ? mPeriodMultiplier.hashCode() : 0);
        result = 31 * result + (mPeriodType != null ? mPeriodType.hashCode() : 0);
        return result;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
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
        dest.writeValue(mOccurenceCount);
        dest.writeValue(mPeriodMultiplier);
        if (mPeriodType != null) {
            dest.writeValue(mPeriodType.name());
        } else {
            dest.writeValue(null);
        }
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
        return mId != null;
    }
}