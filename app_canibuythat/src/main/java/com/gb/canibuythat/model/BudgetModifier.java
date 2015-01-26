package com.gb.canibuythat.model;


/**
 * Created by gbiro on 1/7/2015.
 */

import android.text.TextUtils;

import java.util.Date;

import com.gb.canibuythat.provider.Contract;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;


/**
 * An income or expense that affects the balance of a specified time-span. The user doesn't need to know the exact
 * moment of payment.
 */
@DatabaseTable(tableName = Contract.BudgetModifier.TABLE)
public class BudgetModifier {

	public enum BudgetModifierType {
		UNKNOWN, ACCOMMODATION, AUTOMOBILE, CHILD_SUPPORT, DONATIONS_GIVEN, ENTERTAINMENT, FOOD, GIFTS_GIVEN, GROCERIES, HOUSEHOLD, INSURANCE, MEDICARE, PERSONAL_CARE, PETS, SELF_IMPROVEMENT, SPORTS_RECREATION, TAX, TRANSPORTATION, UTILITIES, VACATION, GIFTS_RECEIVED, INCOME, FINES, ONLINE_SERVICES
	}

	public static enum PeriodType {
		UNKNOWN, DAYS, WEEKS, MONTHS, YEARS
	}

	@DatabaseField(generatedId = true, columnName = Contract.BudgetModifier._ID)
	public Integer				id;

	@DatabaseField(index = true, columnName = Contract.BudgetModifier.TITLE, unique = true)
	public String				title;

	@DatabaseField(columnName = Contract.BudgetModifier.NOTES)
	public String				notes;

	@DatabaseField(columnName = Contract.BudgetModifier.TYPE)
	public BudgetModifierType	type;

	@DatabaseField(columnName = Contract.BudgetModifier.AMOUNT)
	public Float				amount;

	/**
	 * Date before witch the transaction certainly wont happen. The repetition period is added to this date.
	 */
	@DatabaseField(columnName = Contract.BudgetModifier.LOWER_DATE)
	public Date					lowerDate;

	/**
	 * Date by witch the transaction most certainly did happen. The repetition period is added to this date.
	 */
	@DatabaseField(columnName = Contract.BudgetModifier.UPPER_DATE)
	public Date					upperDate;

	/**
	 * How many times this modifier will be spent/cashed in. If 0, the field #periodMultiplier and #period are ignored
	 */
	@DatabaseField(columnName = Contract.BudgetModifier.REPETITION_COUNT)
	public Integer				repetitionCount;

	/**
	 * For periods like every 2 days or once every trimester...
	 */
	@DatabaseField(columnName = Contract.BudgetModifier.PERIOD_MULTIPLIER)
	public Integer				periodMultiplier;

	/**
	 * Does this modifier repeat every day/week/month/year. The first affected time-span (specified by lowerDate and
	 * upperDate) must not be larger the this period.<br>
	 * Ex: The first week of every month, cold months of the year, every weekend, every semester
	 */
	@DatabaseField(columnName = Contract.BudgetModifier.PERIOD)
	public PeriodType			periodType;


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
}