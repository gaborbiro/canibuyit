package com.gb.canibuythat.ui

import android.app.DatePickerDialog
import android.content.Context
import android.support.v7.widget.AppCompatButton
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.DatePicker
import com.gb.canibuythat.util.DateUtils
import java.util.*

class DatePickerButton : AppCompatButton {

    private lateinit var originalDate: Calendar
    private var datePickerDialog: DatePickerDialog? = null
    private lateinit var detector: GestureDetector
    private val onDateSetListener = { _: DatePicker, year: Int, monthOfYear: Int, dayOfMonth: Int ->
        text = DateUtils.formatDayMonthYearWithPrefix(GregorianCalendar(year, monthOfYear, dayOfMonth).time)
    }

    val selectedDate: Date
        get() {
            if (datePickerDialog != null) {
                val datePicker = datePickerDialog!!.datePicker
                return GregorianCalendar(datePicker.year, datePicker.month, datePicker.dayOfMonth).time
            }
            return originalDate.time
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
        originalDate = Calendar.getInstance()
    }

    override fun dispatchTouchEvent(e: MotionEvent): Boolean {
        detector.onTouchEvent(e)
        return super.dispatchTouchEvent(e)
    }

    private fun getDatePickerDialog(): DatePickerDialog {
        if (datePickerDialog == null) {
            datePickerDialog = DateUtils.getDatePickerDialog(context, onDateSetListener, originalDate)
        }
        return datePickerDialog!!
    }

    fun setDate(date: Date) {
        originalDate.time = date
        if (datePickerDialog != null) {
            getDatePickerDialog().updateDate(originalDate.get(Calendar.YEAR),
                    originalDate.get(Calendar.MONTH),
                    originalDate.get(Calendar.DAY_OF_MONTH))
        }
    }
}
