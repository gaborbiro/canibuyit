package com.gb.canibuythat.provider;

import com.gb.canibuythat.model.BudgetItem;
import com.gb.canibuythat.util.DateUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class BalanceCalculator {

    private static BalanceCalculator INSTANCE;

    public static BalanceCalculator get() {
        if (INSTANCE == null) {
            INSTANCE = new BalanceCalculator();
        }
        return INSTANCE;
    }

    /**
     * Calculate how many times the specified <code>budgetItem</code> has been
     * applied up until the specified <code>end</code> (usually today) and sum up
     * it's value.
     *
     * @return two floating point values, the first is the minimum value (because there
     * are periods where the application of the modifier is uncertain), the second is
     * the maximum value
     */
    public BalanceResult getEstimatedBalance(BudgetItem budgetItem, Date start,
            Date end) {
        if (start != null && end != null && !end.after(start)) {
            throw new IllegalArgumentException("Start must come before end!");
        }
        boolean exit = false;
        float bestCase = 0;
        float worstCase = 0;
        List<Date> spendingEvents = new ArrayList<>();
        int count = 0;
        Calendar occurrenceStart = Calendar.getInstance();
        occurrenceStart.setTime(budgetItem.mFirstOccurrenceStart);
        Calendar occurrenceEnd = Calendar.getInstance();
        occurrenceEnd.setTime(budgetItem.mFirstOccurrenceEnd);
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
                    worstCase += budgetItem.mAmount * budgetItem.mType.getSign();
                    if (r > 1) { // after end date
                        bestCase += budgetItem.mAmount * budgetItem.mType.getSign();
                        spendingEvents.add(occurrenceEnd.getTime());
                    }
                } else {
                    exit = true;
                }
            }
            budgetItem.mPeriodType.apply(occurrenceStart, budgetItem.mPeriodMultiplier);
            budgetItem.mPeriodType.apply(occurrenceEnd, budgetItem.mPeriodMultiplier);

            if (budgetItem.mOccurrenceCount != null &&
                    ++count >= budgetItem.mOccurrenceCount) {
                exit = true;
            }
        } while (!exit);
        return new BalanceResult(bestCase, worstCase,
                spendingEvents.toArray(new Date[spendingEvents.size()]));
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
         * For eg if a weekly BudgetItem starts on a Monday, than the array of spending
         * events will be all the Sundays after that Monday and before today (including
         * today).
         */
        public final Date[] spendingEvents;

        public BalanceResult(float bestCase, float worstCase, Date[] spendingEvents) {
            this.bestCase = bestCase;
            this.worstCase = worstCase;
            this.spendingEvents = spendingEvents;
        }
    }
}
