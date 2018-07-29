package com.gb.canibuythat.ui

import android.app.DatePickerDialog
import android.content.Context
import android.support.v7.widget.AppCompatButton
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import com.gb.canibuythat.util.createDatePickerDialog
import com.gb.canibuythat.util.formatDayMonthYearWithPrefix
import java.time.LocalDate

class DatePickerButton : AppCompatButton {

    private lateinit var originalDate: LocalDate
    private var datePickerDialog: DatePickerDialog? = null
    private lateinit var detector: GestureDetector

    val selectedDate: LocalDate
        get() {
            if (datePickerDialog != null) {
                val datePicker = datePickerDialog!!.datePicker
                return LocalDate.of(datePicker.year, datePicker.month + 1, datePicker.dayOfMonth)
            }
            return originalDate
        }

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }

    private fun init() {
        detector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                getDatePickerDialog().show()
                return false
            }
        })
        originalDate = LocalDate.now()
    }

    override fun dispatchTouchEvent(e: MotionEvent): Boolean {
        detector.onTouchEvent(e)
        return super.dispatchTouchEvent(e)
    }

    private fun getDatePickerDialog(): DatePickerDialog {
        if (datePickerDialog == null) {
            datePickerDialog = createDatePickerDialog(context, originalDate) { _, date ->
                text = date.formatDayMonthYearWithPrefix()
            }
        }
        return datePickerDialog!!
    }

    fun setDate(date: LocalDate) {
        originalDate = date
        if (datePickerDialog != null) {
            getDatePickerDialog().updateDate(originalDate.year, originalDate.monthValue, originalDate.dayOfMonth)
        }
    }
}
