package com.gb.canibuythat.provider;

import com.gb.canibuythat.model.BudgetItem;
import com.gb.canibuythat.util.DateUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class BalanceCalculator {

    /**
     * Calculate how many times the specified <code>budgetModifier</code> has been
     * applied in the past (up until today) and sum up it's value.
     *
     * @return two floating point values, the first is the minimum value (because there
     * are periods where the application of the modifier is uncertain), the second is
     * the maximum value
     */
    public static BalanceResult getEstimatedBalance(BudgetItem budgetItem,
            Date startingFrom) {
        boolean exit = false;
        float bestCase = 0;
        float worstCase = 0;
        List<Date> spendingEvents = new ArrayList<>();
        int count = 0;
        Calendar occurrenceStart = Calendar.getInstance();
        occurrenceStart.setTime(budgetItem.mFirstOccurrenceStart);
        Calendar occurrenceEnd = Calendar.getInstance();
        occurrenceEnd.setTime(budgetItem.mFirstOccurrenceEnd);
        Calendar startDate = Calendar.getInstance();

        if (startingFrom != null) {
            startDate.setTime(startingFrom);
        }
        startDate = DateUtils.clearLowerBits(startDate);

        do {
            int r = DateUtils.compare(startDate, occurrenceStart, occurrenceEnd);

            if (r >= -1) {//after or on start date
                worstCase += budgetItem.mAmount * budgetItem.mType.getSign();
                if (r > 1) { // after end date
                    bestCase += budgetItem.mAmount * budgetItem.mType.getSign();
                    spendingEvents.add(occurrenceEnd.getTime());
                }
                budgetItem.mPeriodType.apply(occurrenceStart,
                        budgetItem.mPeriodMultiplier);
                budgetItem.mPeriodType.apply(occurrenceEnd, budgetItem.mPeriodMultiplier);
                count++;

                if (budgetItem.mOccurenceCount != null &&
                        count >= budgetItem.mOccurenceCount) {
                    exit = true;
                }
            } else {
                exit = true;
            }
        } while (!exit);
        return new BalanceResult(bestCase, worstCase,
                spendingEvents.toArray(new Date[spendingEvents.size()]));
    }

    public static class BalanceResult {
        public float bestCase;
        public float worstCase;
        public Date[] spendingEvents;

        public BalanceResult(float bestCase, float worstCase, Date[] spendingEvents) {
            this.bestCase = bestCase;
            this.worstCase = worstCase;
            this.spendingEvents = spendingEvents;
        }
    }
}
