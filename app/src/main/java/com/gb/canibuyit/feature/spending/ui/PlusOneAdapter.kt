package com.gb.canibuyit.feature.spending.ui

import android.app.Activity
import android.widget.ArrayAdapter

class PlusOneAdapter constructor(activity: Activity, items: Array<*>) :
    ArrayAdapter<Any>(activity, android.R.layout.simple_list_item_1, items) {

    override fun getCount() = super.getCount() + 1

    override fun getItem(position: Int): Any? {
        return if (position == 0) "Select one" else super.getItem(position - 1)
    }

    override fun getItemId(position: Int): Long {
        return if (position == 0) -1 else super.getItemId(position - 1)
    }
}