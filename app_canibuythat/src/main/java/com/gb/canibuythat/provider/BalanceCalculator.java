package com.gb.canibuythat.provider;

import com.gb.canibuythat.model.Spending;
import com.gb.canibuythat.util.DateUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class BalanceCalculator {

    /**
     * Calculate how many times the specified <code>spending</code> has been
     * applied up until the specified <code>end</code> date (usually today) and sum up
     * it's value.
     *
     * @param end if null, `today` is used instead
     * @return two floating point values, the first is the minimum possible value, the second is
     * the maximum possible value
     */
    public static BalanceResult getEstimatedBalance(Spending spending, Date start, Date end) {
        if (start != null && end != null && !end.after(start)) {
            throw new IllegalArgumentException("Start date must come before end date!");
        }
        boolean exit = false;
        float bestCase = 0;
        float worstCase = 0;
        List<Date> spendingEvents = new ArrayList<>();
        int count = 0;
        Calendar occurrenceStart = Calendar.getInstance();
        occurrenceStart.setTime(spending.getFromStartDate());
        Calendar occurrenceEnd = Calendar.getInstance();
        occurrenceEnd.setTime(spending.getFromEndDate());
        Calendar date = Calendar.getInstance();
        if (end != null) {
            date.setTime(end);
        }
        DateUtils.clearLowerBits(date);
        start = start != null ? DateUtils.clearLowerBits(start) : null;
        do {
            if (start == null || occurrenceEnd.getTimeInMillis() >= start.getTime()) {
                int r = DateUtils.compare(date, occurrenceStart, occurrenceEnd);
                if (r >= -1) { //after or on start date
                    if (spending.getEnabled()) {
                        worstCase += spending.getValue();
                    }
                    if (r > 1) { // after end date
                        if (spending.getEnabled()) {
                            bestCase += spending.getValue();
                        }
                        spendingEvents.add(occurrenceEnd.getTime());
                    }
                } else {
                    exit = true;
                }
            }
            spending.getCycle().apply(occurrenceStart, spending.getCycleMultiplier());
            spending.getCycle().apply(occurrenceEnd, spending.getCycleMultiplier());
            if (spending.getOccurrenceCount() != null && ++count >= spending.getOccurrenceCount()) {
                exit = true;
            }
        } while (!exit);
        return new BalanceResult(bestCase, worstCase, spendingEvents.toArray(new Date[spendingEvents.size()]));
    }

    public static class BalanceResult {
        /**
         * Minimum amount of money that is expected to be spent until now (according to
         * the user's spending habits) on a particular spending category. The
         * difference between minimum and maximum comes from the fact that a
         * SpendingItem may be spent in an interval rather than on an exact date.
         */
        public final float bestCase;
        /**
         * Maximum amount of money that is expected to be spent until now (according to
         * the user's spending habits) on a particular spending category. The
         * difference between minimum and maximum comes from the fact that a
         * SpendingItem may be spent in an interval rather than on an exact date.<br>
         */
        public final float worstCase;
        /**
         * Spending event is the latest day on which we expect a payment/income to happen.
         * For eg if a weekly Spending starts on a Monday, than the array of spending
         * events will be all the Sundays after that Monday and before today (including
         * today).
         */
        public final Date[] spendingEvents;

        BalanceResult(float bestCase, float worstCase, Date[] spendingEvents) {
            this.bestCase = bestCase;
            this.worstCase = worstCase;
            this.spendingEvents = spendingEvents;
        }
    }
}
