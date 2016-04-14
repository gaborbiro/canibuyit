package com.gb.canibuythat.provider;

import com.gb.canibuythat.model.BudgetItem;
import com.gb.canibuythat.util.DateUtils;

import java.util.Calendar;
import java.util.Date;

public class BalanceCalculator {

    public static void increaseDateWithPeriod(Calendar c, BudgetItem.PeriodType period,
            int periodMultiplier) {
        switch (period) {
            case DAYS:
                c.add(Calendar.DAY_OF_MONTH, periodMultiplier);
                break;
            case WEEKS:
                c.add(Calendar.WEEK_OF_MONTH, periodMultiplier);
                break;
            case MONTHS:
                c.add(Calendar.MONTH, periodMultiplier);
                break;
            case YEARS:
                c.add(Calendar.YEAR, periodMultiplier);
                break;
        }
    }

    /**
     * Calculate how many times the specified <code>budgetModifier</code> has been
     * applied in the past (up until today) and sum up it's value.
     *
     * @return two floating point values, the first is the minimum value (because there
     * are periods where the application of the modifier is uncertain), the second is
     * the maximum value
     */
    public float[] getEstimatedBalance(BudgetItem budgetItem, Date startingFrom) {
        boolean exit = false;
        float minimum = 0;
        float maximum = 0;
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
            if (beforeBIOccurrence(startDate, occurrenceStart)) {
                exit = true;
            } else if (inBIOccurrence(startDate, occurrenceStart, occurrenceEnd)) {
                maximum += budgetItem.mAmount * budgetItem.mType.getSign();
            } else if (afterBIOccurrence(startDate, occurrenceEnd)) {
                minimum += budgetItem.mAmount * budgetItem.mType.getSign();
                maximum += budgetItem.mAmount * budgetItem.mType.getSign();
            }
            if (!exit) {
                increaseDateWithPeriod(occurrenceStart, budgetItem.mPeriodType,
                        budgetItem.mPeriodMultiplier);
                increaseDateWithPeriod(occurrenceEnd, budgetItem.mPeriodType,
                        budgetItem.mPeriodMultiplier);
                count++;

                if (budgetItem.mOccurenceCount != null &&
                        count >= budgetItem.mOccurenceCount) {
                    exit = true;
                }
            }
        } while (!exit);
        return new float[]{minimum, maximum};
    }

    boolean beforeBIOccurrence(Calendar now, Calendar occurrenceStart) {
        return now.before(occurrenceStart);
    }

    /**
     * Boundaries included
     */
    boolean inBIOccurrence(Calendar now, Calendar occurrenceStart,
            Calendar occurrenceEnd) {
        return now.getTimeInMillis() >= occurrenceStart.getTimeInMillis() &&
                now.getTimeInMillis() <= occurrenceEnd.getTimeInMillis();
    }

    boolean afterBIOccurrence(Calendar now, Calendar occurrenceEnd) {
        return now.after(occurrenceEnd);
    }
}
