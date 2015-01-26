package com.gb.canibuythat.provider;


import android.provider.BaseColumns;


/**
 * If you add a new column to a table, don't forget to add it to the COLUMNS array as well.
 * <p/>
 * Created by GABOR on 2015-jan.-24.
 */
public class Contract {

	public static class BudgetModifier implements BaseColumns {

		public static final String	TABLE					= "budget_modifier";

		public static final String	TITLE					= "title";
		public static final String	NOTES					= "notes";
		public static final String	AMOUNT					= "amount";
		public static final String	TYPE					= "type";
		public static final String	LOWER_DATE				= "lower_date";
		public static final String	UPPER_DATE				= "upper_date";
		public static final String	REPETITION_COUNT		= "repetition_count";
		public static final String	PERIOD_MULTIPLIER		= "period_multiplier";
		public static final String	PERIOD					= "period";

		public static final String[] COLUMNS = {_ID, TITLE, NOTES, AMOUNT, TYPE, LOWER_DATE, UPPER_DATE, REPETITION_COUNT, PERIOD_MULTIPLIER, PERIOD};
	}
}
