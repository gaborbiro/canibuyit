package com.gb.canibuythat.ui;

import android.app.DatePickerDialog;
import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.gb.canibuythat.R;
import com.gb.canibuythat.util.DateUtils;

import java.util.Calendar;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;

public class DateRangePicker extends LinearLayout {

    public interface TouchInterceptor {
        boolean onInterceptTouchEvent(MotionEvent ev);
    }

    @BindView(R.id.start_date) Button startDateBtn;
    @BindView(R.id.end_date) Button endDateBtn;
    @BindView(R.id.reset) ImageView resetBtn;

    private Date startDate;
    private Date endDate;

    private TouchInterceptor touchInterceptor;

    private DatePickerDialog.OnDateSetListener dateSetListener = new DatePickerDialog.OnDateSetListener() {
        @Override
        public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
            Calendar newDate = DateUtils.compose(year, month, dayOfMonth);

            switch ((int) view.getTag()) {
                case R.id.start_date:
                    startDate = newDate.getTime();
                    startDateBtn.setText(DateUtils.getFORMAT_MONTH_DAY_YR().format(newDate.getTime()));
                    if (endDate.getTime() < newDate.getTime().getTime()) {
                        endDatePickerDialog = new DatePickerDialog(getContext(),
                                dateSetListener, DateUtils.decompose(newDate)[0], DateUtils.decompose(newDate)[1], DateUtils.decompose(newDate)[2]);
                        setEndDate(newDate.getTime());
                    }
                    startDateChanged = true;
                    break;
                case R.id.end_date:
                    endDate = newDate.getTime();
                    endDateBtn.setText(DateUtils.getFORMAT_MONTH_DAY_YR().format(newDate.getTime()));
                    if (startDate.getTime() > newDate.getTime().getTime()) {
                        startDatePickerDialog = new DatePickerDialog(getContext(),
                                dateSetListener, DateUtils.decompose(newDate)[0], DateUtils.decompose(newDate)[1], DateUtils.decompose(newDate)[2]);
                        setStartDate(newDate.getTime());
                    }
                    endDateChanged = true;
                    break;
            }
        }
    };
    private View.OnClickListener datePickerOnClickListener = v -> {
        switch (v.getId()) {
            case R.id.start_date:
                getStartDatePickerDialog().show();
                startDateBtn.setError(null);
                break;
            case R.id.end_date:
                getEndDatePickerDialog().show();
                endDateBtn.setError(null);
                break;
        }
    };
    private boolean startDateChanged;
    private boolean endDateChanged;
    private DatePickerDialog startDatePickerDialog;
    private DatePickerDialog endDatePickerDialog;

    public DateRangePicker(Context context) {
        super(context);
        init();
    }

    public DateRangePicker(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DateRangePicker(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        inflate(getContext(), R.layout.date_range_picker, this);
        ButterKnife.bind(this);
        resetDates();
        startDateBtn.setOnClickListener(datePickerOnClickListener);
        endDateBtn.setOnClickListener(datePickerOnClickListener);
        resetBtn.setOnClickListener(v -> {
            resetDates();
            startDatePickerDialog = null;
            endDatePickerDialog = null;
        });
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
        updateButtons();
        startDatePickerDialog = null;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
        updateButtons();
        endDatePickerDialog = null;
    }

    public Date getStartDate() {
        return startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public boolean isStartDateChanged() {
        return startDateChanged;
    }

    public boolean isEndDateChanged() {
        return endDateChanged;
    }

    private void resetDates() {
        Calendar c = Calendar.getInstance();
        DateUtils.clearLowerBits(c);
        startDate = c.getTime();
        endDate = c.getTime();
        updateButtons();
    }

    private void updateButtons() {
        startDateBtn.setText(DateUtils.getFORMAT_MONTH_DAY_YR().format(this.startDate));
        endDateBtn.setText(DateUtils.getFORMAT_MONTH_DAY_YR().format(this.endDate));
    }

    private DatePickerDialog getStartDatePickerDialog() {
        if (startDatePickerDialog == null) {
            startDatePickerDialog = new DatePickerDialog(getContext(), dateSetListener,
                    DateUtils.decompose(startDate)[0], DateUtils.decompose(startDate)[1], DateUtils.decompose(startDate)[2]);
        }
        startDatePickerDialog.getDatePicker().setTag(R.id.start_date);
        return startDatePickerDialog;
    }

    private DatePickerDialog getEndDatePickerDialog() {
        if (endDatePickerDialog == null) {
            endDatePickerDialog = new DatePickerDialog(getContext(), dateSetListener,
                    DateUtils.decompose(endDate)[0], DateUtils.decompose(endDate)[1], DateUtils.decompose(endDate)[2]);
        }
        endDatePickerDialog.getDatePicker().setTag(R.id.end_date);
        return endDatePickerDialog;
    }

    public void setTouchInterceptor(TouchInterceptor touchInterceptor) {
        this.touchInterceptor = touchInterceptor;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (touchInterceptor != null) {
            return touchInterceptor.onInterceptTouchEvent(ev);
        } else {
            return super.onInterceptTouchEvent(ev);
        }
    }
}
