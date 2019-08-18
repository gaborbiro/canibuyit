package com.gb.canibuyit.util

import android.content.Context
import android.util.TypedValue
import androidx.core.content.ContextCompat

fun Context.themeAttributeToColor(themeAttributeId: Int, defaultColorId: Int): Int {
    val outValue = TypedValue()
    val theme = theme
    val wasResolved = theme.resolveAttribute(themeAttributeId, outValue, true)
    return if (wasResolved) {
        ContextCompat.getColor(this, outValue.resourceId)
    } else {
        defaultColorId
    }
}