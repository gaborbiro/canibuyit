package com.gb.canibuythat.ui;

import android.app.DatePickerDialog;
import android.content.Context;
import android.support.v7.widget.AppCompatButton;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.DatePicker;

import com.gb.canibuythat.util.DateUtils;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class DatePickerButton extends AppCompatButton {

    private Calendar originalDate;
    private DatePickerDialog datePickerDialog;
    private GestureDetector detector;
    private DatePickerDialog.OnDateSetListener onDateSetListener = (view, year, monthOfYear, dayOfMonth) ->
            setText(DateUtils.formatDayMonthYear(new GregorianCalendar(year, monthOfYear, dayOfMonth).getTime()));

    public DatePickerButton(Context context) {
        super(context);
        init();
    }

    public DatePickerButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DatePickerButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        detector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                getDatePickerDialog().show();
                return false;
            }
        });
        originalDate = Calendar.getInstance();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent e) {
        detector.onTouchEvent(e);
        return super.dispatchTouchEvent(e);
    }

    private DatePickerDialog getDatePickerDialog() {
        if (datePickerDialog == null) {
            datePickerDialog = DateUtils.getDatePickerDialog(getContext(), onDateSetListener, originalDate);
        }
        return datePickerDialog;
    }

    public void setDate(Date date) {
        originalDate.setTime(date);
        if (datePickerDialog != null) {
            getDatePickerDialog().updateDate(originalDate.get(Calendar.YEAR),
                    originalDate.get(Calendar.MONTH),
                    originalDate.get(Calendar.DAY_OF_MONTH));
        }
    }

    public Date getSelectedDate() {
        if (datePickerDialog != null) {
            DatePicker datePicker = datePickerDialog.getDatePicker();
            return new GregorianCalendar(datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth()).getTime();
        }
        return originalDate.getTime();
    }
}
