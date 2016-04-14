package com.gb.canibuythat.provider;

import android.provider.BaseColumns;

/**
 * If you add a new column to a table, don't forget to add it to the COLUMNS array as
 * well.
 */
public class Contract {

    public static class BudgetItem implements BaseColumns {

        public static final String TABLE = "budget_item";

        public static final String NAME = "name";
        public static final String NOTES = "notes";
        public static final String AMOUNT = "amount";
        public static final String TYPE = "type";
        public static final String FIRST_OCCURRENCE_START = "first_occurrence_start";
        public static final String FIRST_OCCURRENCE_END = "first_occurrence_end";
        public static final String OCCURRENCE_COUNT = "occurrence_count";
        public static final String PERIOD_MULTIPLIER = "period_multiplier";
        public static final String PERIOD_TYPE = "period_type";

        public static final String[] COLUMNS =
                {_ID, NAME, NOTES, AMOUNT, TYPE, FIRST_OCCURRENCE_START,
                        FIRST_OCCURRENCE_END, OCCURRENCE_COUNT, PERIOD_MULTIPLIER,
                        PERIOD_TYPE};
    }

    public static class BalanceUpdateEvent implements BaseColumns {

        public static final String TABLE = "balance_update_event";

        public static final String WHEN = "when";
        public static final String VALUE = "value";

        public static final String[] COLUMNS = {_ID, WHEN, VALUE};
    }
}
