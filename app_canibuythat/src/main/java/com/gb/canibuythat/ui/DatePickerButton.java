package com.gb.canibuythat.ui;

import android.app.DatePickerDialog;
import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.DatePicker;

import com.gb.canibuythat.util.DateUtils;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class DatePickerButton extends Button {

    private Calendar mOriginalDate;
    private DatePickerDialog mDatePickerDialog;
    private GestureDetector mDetector;
    private DatePickerDialog.OnDateSetListener mOnDateSetListener =
            new DatePickerDialog.OnDateSetListener() {

                @Override
                public void onDateSet(DatePicker view, int year, int monthOfYear,
                        int dayOfMonth) {
                    setText(DateUtils.FORMAT_MONTH_DAY.format(
                            new GregorianCalendar(year, monthOfYear,
                                    dayOfMonth).getTime()));
                }
            };

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
        mDetector = new GestureDetector(getContext(),
                new GestureDetector.SimpleOnGestureListener() {

                    @Override public boolean onSingleTapConfirmed(MotionEvent e) {
                        getDatePickerDialog().show();
                        return false;
                    }
                });
        mOriginalDate = Calendar.getInstance();
    }

    @Override public boolean dispatchTouchEvent(MotionEvent e) {
        mDetector.onTouchEvent(e);
        return super.dispatchTouchEvent(e);
    }

    private DatePickerDialog getDatePickerDialog() {
        if (mDatePickerDialog == null) {
            mDatePickerDialog = DateUtils.getDatePickerDialog(getContext(), mOnDateSetListener,
                    mOriginalDate);
        }
        return mDatePickerDialog;
    }

    public void setDate(Date date) {
        mOriginalDate.setTime(date);

        if (mDatePickerDialog != null) {
            getDatePickerDialog().updateDate(mOriginalDate.get(Calendar.YEAR),
                    mOriginalDate.get(Calendar.MONTH),
                    mOriginalDate.get(Calendar.DAY_OF_MONTH));
        }
    }

    public Date getSelectedDate() {
        if (mDatePickerDialog != null) {
            DatePicker datePicker = mDatePickerDialog.getDatePicker();
            return new GregorianCalendar(datePicker.getYear(), datePicker.getMonth(),
                    datePicker.getDayOfMonth()).getTime();
        }
        return mOriginalDate.getTime();
    }
}
