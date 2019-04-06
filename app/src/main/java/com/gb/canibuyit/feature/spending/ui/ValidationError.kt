package com.gb.canibuyit.feature.spending.ui

import android.content.Context
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.IntDef

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * There are two kinds of validation errors: the ones that can be shown in an input
 * field (with [TextView.setError] and the ones tha are shown
 * as a Toast or Dialog instead (DatePicker, etc...).
 */
class ValidationError(@param:ValidationErrorType
                      internal val type: Int, internal val target: View?,
                      internal val errorMessage: String) : Exception() {

    @IntDef(TYPE_INPUT_FIELD,
            TYPE_NON_INPUT_FIELD)
    @Retention(RetentionPolicy.SOURCE)
    private annotation class ValidationErrorType

    init {
        if (type == TYPE_INPUT_FIELD) {
            if (target == null) {
                throw IllegalArgumentException(
                        "Please specify a target view for the input field error " + "message")
            }
            if (target !is TextView) {
                throw IllegalArgumentException(
                        "Wrong view type in ValidationError. Cannot show error " + "message.")

            }
        }
    }

    @Throws(IllegalArgumentException::class)
    fun showError(context: Context) {
        if (type == TYPE_INPUT_FIELD) {
            val textView = target as TextView
            textView.error = errorMessage
            textView.requestFocus()
        } else if (type == TYPE_NON_INPUT_FIELD) {
            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
            target?.requestFocus()
        }
    }

    companion object {
        const val TYPE_INPUT_FIELD = 1
        const val TYPE_NON_INPUT_FIELD = 2
    }
}