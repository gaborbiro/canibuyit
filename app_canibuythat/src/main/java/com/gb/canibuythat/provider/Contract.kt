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

            const val _ID = BaseColumns._ID
            const val NAME = "name"
            const val NOTES = "notes"
            const val VALUE = "value"
            const val TYPE = "type"
            const val FROM_START_DATE = "from_start_date"
            const val FROM_END_DATE = "from_end_date"
            const val OCCURRENCE_COUNT = "occurrence_count"
            const val CYCLE_MULTIPLIER = "cycle_multiplier"
            const val CYCLE = "cycle"
            const val ENABLED = "enabled"
            const val SOURCE_DATA = "source_data"
            const val SPENT = "spent"
            const val TARGET = "target"

            val COLUMNS = arrayOf(_ID, NAME, NOTES, VALUE, TYPE, FROM_START_DATE, FROM_END_DATE, OCCURRENCE_COUNT, CYCLE_MULTIPLIER, CYCLE, ENABLED, SOURCE_DATA, SPENT, TARGET)
        }
    }

    class Project {
        companion object {
            const val TABLE = "project"
            const val _ID = BaseColumns._ID
            const val NAME = "name"
        }
    }
}
