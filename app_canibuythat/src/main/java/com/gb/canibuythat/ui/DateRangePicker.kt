package com.gb.canibuythat.ui

import android.app.DatePickerDialog
import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.DatePicker
import android.widget.LinearLayout
import butterknife.ButterKnife
import com.gb.canibuythat.R
import com.gb.canibuythat.util.createDatePickerDialog
import com.gb.canibuythat.util.formatDayMonthYear
import kotlinx.android.synthetic.main.date_range_picker.view.*
import java.time.LocalDate

class DateRangePicker : LinearLayout {

    var startDate: LocalDate = LocalDate.now()
        set(value) {
            field = value
            start_date_btn.text = value.formatDayMonthYear()
            startDatePickerDialog = null
        }

    var endDate: LocalDate = LocalDate.now()
        set(value) {
            field = value
            end_date_btn.text = value.formatDayMonthYear()
            endDatePickerDialog = null
        }

    var touchInterceptor: ((ev: MotionEvent) -> Boolean)? = null

    private val onDateSetListener: (DatePicker, LocalDate) -> Unit = { datePicker, date ->
        when (datePicker.tag as Int) {
            R.id.start_date_btn -> {
                startDate = date
                if (endDate < date) {
                    endDate = date
                }
                isStartDateChanged = true
            }
            R.id.end_date_btn -> {
                endDate = date
                if (startDate > date) {
                    startDate = date
                }
                isEndDateChanged = true
            }
        }
    }

    private val datePickerOnClickListener = { v: View ->
        when (v.id) {
            R.id.start_date_btn -> {
                getStartDatePickerDialog().show()
                start_date_btn.error = null
            }
            R.id.end_date_btn -> {
                getEndDatePickerDialog().show()
                end_date_btn.error = null
            }
        }
    }
    var isStartDateChanged: Boolean = false
        private set
    var isEndDateChanged: Boolean = false
        private set
    private var startDatePickerDialog: DatePickerDialog? = null
    private var endDatePickerDialog: DatePickerDialog? = null

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    private fun init() {
        View.inflate(context, R.layout.date_range_picker, this)
        orientation = LinearLayout.VERTICAL
        ButterKnife.bind(this)
        resetDates()

        start_date_btn.setOnClickListener(datePickerOnClickListener)
        end_date_btn.setOnClickListener(datePickerOnClickListener)
        reset_btn.setOnClickListener { v ->
            resetDates()
            startDatePickerDialog = null
            endDatePickerDialog = null
        }
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }

    private fun resetDates() {
        startDate = LocalDate.now()
        start_date_btn.text = startDate.formatDayMonthYear()
        endDate = LocalDate.now()
        end_date_btn.text = endDate.formatDayMonthYear()
    }

    private fun getStartDatePickerDialog(): DatePickerDialog {
        startDatePickerDialog = startDatePickerDialog ?: createDatePickerDialog(context, startDate, onDateSetListener).apply {
            datePicker.tag = R.id.start_date_btn
        }
        return startDatePickerDialog!!
    }

    private fun getEndDatePickerDialog(): DatePickerDialog {
        endDatePickerDialog = endDatePickerDialog ?: createDatePickerDialog(context, endDate, onDateSetListener).apply {
            datePicker.tag = R.id.end_date_btn
        }
        return endDatePickerDialog!!
    }

    override fun onInterceptTouchEvent(ev: MotionEvent) = touchInterceptor?.invoke(ev)
            ?: super.onInterceptTouchEvent(ev)
}
