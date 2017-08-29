package com.gb.canibuythat.provider

import android.provider.BaseColumns

/**
 * If you add a new column to a table, don't forget to add it to the COLUMNS array as
 * well.
 */
class Contract {
    class Spending {
        companion object {

            const val TABLE = "spending"

            const val _ID = "_id"
            const val NAME = "name"
            const val NOTES = "notes"
            const val AMOUNT = "amount"
            const val TYPE = "type"
            const val FIRST_OCCURRENCE_START = "first_occurrence_start"
            const val FIRST_OCCURRENCE_END = "first_occurrence_end"
            const val OCCURRENCE_COUNT = "occurrence_count"
            const val PERIOD_MULTIPLIER = "period_multiplier"
            const val PERIOD_TYPE = "period_type"
            const val ENABLED = "enabled"
            const val SOURCE_DATA = "source_data"
            const val SPENT = "spent"
            const val TARGET = "target"

            val COLUMNS = arrayOf(BaseColumns._ID, NAME, NOTES, AMOUNT, TYPE, FIRST_OCCURRENCE_START, FIRST_OCCURRENCE_END, OCCURRENCE_COUNT, PERIOD_MULTIPLIER, PERIOD_TYPE, ENABLED, SOURCE_DATA, SPENT, TARGET)
        }
    }
}
