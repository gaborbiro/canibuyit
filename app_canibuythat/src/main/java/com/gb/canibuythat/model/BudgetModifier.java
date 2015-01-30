package com.gb.canibuythat.model;


/**
 * Created by gbiro on 1/7/2015.
 */

import android.os.Parcel;
import android.os.Parcelable;

import com.gb.canibuythat.provider.Contract;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Date;


/**
 * An income or expense that affects the balance of a specified time-span. The user doesn't need to know the exact
 * moment of payment.
 */
@DatabaseTable(tableName = Contract.BudgetModifier.TABLE)
public class BudgetModifier implements Parcelable {

    public enum BudgetModifierType {
        ACCOMMODATION(-1), AUTOMOBILE(-1), CHILD_SUPPORT(-1), DONATIONS_GIVEN(-1), ENTERTAINMENT(-1),
        FOOD(-1), GIFTS_GIVEN(-1), GROCERIES(-1), HOUSEHOLD(-1), INSURANCE(-1), MEDICARE(-1),
        PERSONAL_CARE(-1), PETS(-1), SELF_IMPROVEMENT(-1), SPORTS_RECREATION(-1), TAX(-1), TRANSPORTATION(-1),
        UTILITIES(-1), VACATION(-1), GIFTS_RECEIVED(1), INCOME(1), FINES(-1), ONLINE_SERVICES(-1);

        final byte sign;

        BudgetModifierType(int sign) {
            this.sign = (byte) sign;
        }
    }

    public static enum PeriodType {
        DAYS, WEEKS, MONTHS, YEARS
    }

    @DatabaseField(generatedId = true, columnName = Contract.BudgetModifier._ID)
    public Integer id;

    @DatabaseField(index = true, columnName = Contract.BudgetModifier.TITLE, unique = true, canBeNull = false)
    public String title;

    @DatabaseField(columnName = Contract.BudgetModifier.NOTES)
    public String notes;

    @DatabaseField(columnName = Contract.BudgetModifier.TYPE, canBeNull = false)
    public BudgetModifierType type;

    @DatabaseField(columnName = Contract.BudgetModifier.AMOUNT, canBeNull = false)
    public Float amount;

    /**
     * Date before witch the transaction certainly wont happen. The repetition period is added to this date.
     */
    @DatabaseField(columnName = Contract.BudgetModifier.LOWER_DATE, canBeNull = false)
    public Date lowerDate;

    /**
     * Date by witch the transaction most certainly did happen. The repetition period is added to this date.
     */
    @DatabaseField(columnName = Contract.BudgetModifier.UPPER_DATE, canBeNull = false)
    public Date upperDate;

    /**
     * How many times this modifier will be spent/cashed in. If 0, the field #periodMultiplier and #period are ignored
     */
    @DatabaseField(columnName = Contract.BudgetModifier.REPETITION_COUNT)
    public Integer repetitionCount;

    /**
     * For periods like every 2 days or once every trimester...
     */
    @DatabaseField(columnName = Contract.BudgetModifier.PERIOD_MULTIPLIER)
    public Integer periodMultiplier;

    /**
     * Does this modifier repeat every day/week/month/year. The first affected time-span (specified by lowerDate and
     * upperDate) must not be larger the this period.<br>
     * Ex: The first week of every month, cold months of the year, every weekend, every semester
     */
    @DatabaseField(columnName = Contract.BudgetModifier.PERIOD, canBeNull = false)
    public PeriodType periodType;

    public BudgetModifier() {
    }

    private BudgetModifier(Parcel in) {
        id = (Integer) in.readValue(Integer.class.getClassLoader());
        title = (String) in.readValue(String.class.getClassLoader());
        amount = (Float) in.readValue(Float.class.getClassLoader());
        notes = (String) in.readValue(String.class.getClassLoader());
        try {
            type = BudgetModifierType.valueOf((String) in.readValue(String.class.getClassLoader()));
        } catch (IllegalArgumentException e) {
            // it means the original value was null
        }
        Long lowerDate = (Long) in.readValue(Long.class.getClassLoader());

        if (lowerDate != null) {
            this.lowerDate = new Date(lowerDate);
        }
        Long upperDate = (Long) in.readValue(Long.class.getClassLoader());

        if (upperDate != null) {
            this.upperDate = new Date(upperDate);
        }
        repetitionCount = (Integer) in.readValue(Integer.class.getClassLoader());
        periodMultiplier = (Integer) in.readValue(Integer.class.getClassLoader());
        try {
            periodType = PeriodType.valueOf((String) in.readValue(String.class.getClassLoader()));
        } catch (IllegalArgumentException e) {
            // it means the original value was null
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        BudgetModifier that = (BudgetModifier) o;

        if (amount != null ? !amount.equals(that.amount) : that.amount != null)
            return false;
        if (lowerDate != null ? !lowerDate.equals(that.lowerDate) : that.lowerDate != null)
            return false;
        if (notes != null ? !notes.equals(that.notes) : that.notes != null)
            return false;
        if (periodMultiplier != null ? !periodMultiplier.equals(that.periodMultiplier) : that.periodMultiplier != null)
            return false;
        if (periodType != that.periodType)
            return false;
        if (repetitionCount != null ? !repetitionCount.equals(that.repetitionCount) : that.repetitionCount != null)
            return false;
        if (title != null ? !title.equals(that.title) : that.title != null)
            return false;
        if (type != that.type)
            return false;
        if (upperDate != null ? !upperDate.equals(that.upperDate) : that.upperDate != null)
            return false;

        return true;
    }


    @Override
    public int hashCode() {
        int result = title != null ? title.hashCode() : 0;
        result = 31 * result + (notes != null ? notes.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (amount != null ? amount.hashCode() : 0);
        result = 31 * result + (lowerDate != null ? lowerDate.hashCode() : 0);
        result = 31 * result + (upperDate != null ? upperDate.hashCode() : 0);
        result = 31 * result + (repetitionCount != null ? repetitionCount.hashCode() : 0);
        result = 31 * result + (periodMultiplier != null ? periodMultiplier.hashCode() : 0);
        result = 31 * result + (periodType != null ? periodType.hashCode() : 0);
        return result;
    }


    @Override
    public int describeContents() {
        return 0;
    }


    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeValue(id);
        dest.writeValue(title);
        dest.writeValue(amount);
        dest.writeValue(notes);
        if (type != null) {
            dest.writeValue(type.name());
        } else {
            dest.writeValue(null);
        }
        if (lowerDate != null) {
            dest.writeLong(lowerDate.getTime());
        } else {
            dest.writeValue(null);
        }
        if (upperDate != null) {
            dest.writeLong(upperDate.getTime());
        } else {
            dest.writeValue(null);
        }
        dest.writeValue(repetitionCount);
        dest.writeValue(periodMultiplier);
        if (periodType != null) {
            dest.writeValue(periodType.name());
        } else {
            dest.writeValue(null);
        }
    }

    public static final Parcelable.Creator<BudgetModifier> CREATOR
            = new Parcelable.Creator<BudgetModifier>() {

        @Override
        public BudgetModifier createFromParcel(Parcel in) {
            return new BudgetModifier(in);
        }

        @Override
        public BudgetModifier[] newArray(int size) {
            return new BudgetModifier[size];
        }
    };
}