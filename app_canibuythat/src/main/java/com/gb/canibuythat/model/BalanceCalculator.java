package com.gb.canibuythat.model;

import java.util.Calendar;

/**
 * Created by gbiro on 1/30/2015.
 */
public class BalanceCalculator {

    private BudgetModifier budgetModifier;

    public BalanceCalculator(BudgetModifier budgetModifier) {
        this.budgetModifier = budgetModifier;
    }


    /**
     * See how many times the specified <code>budgetModifier</code> has been applied in the past (up until today) and sum up it's value.
     *
     * @return two floating point values, the first is the minimum value (because there are periods where the application of the modifier is uncertain), the second is the maximum value
     */
    public float[] getEstimatedBalance(BudgetModifier budgetModifier) {
        boolean exit = false;
        float minimum = 0;
        float maximum = 0;
        int count = 0;
        Calendar c1 = Calendar.getInstance();
        c1.setTime(budgetModifier.lowerDate);
        Calendar c2 = Calendar.getInstance();
        c2.setTime(budgetModifier.upperDate);
        Calendar now = Calendar.getInstance();

        do {
            if (areWeBeforeBMStart(now, c1)) {
                exit = true;
            } else if (areWeWithinBMPeriod(now, c1, c2)) {
                maximum += budgetModifier.amount * budgetModifier.type.sign;
            } else if (areWeAfterBmEnd(now, c2)) {
                minimum += budgetModifier.amount * budgetModifier.type.sign;
                maximum += budgetModifier.amount * budgetModifier.type.sign;
            }
            increaseDateWithPeriod(c1, budgetModifier.periodType, budgetModifier.periodMultiplier);
            increaseDateWithPeriod(c2, budgetModifier.periodType, budgetModifier.periodMultiplier);
            count++;

            if (budgetModifier.repetitionCount != null && count >= budgetModifier.repetitionCount) {
                exit = true;
            }
        } while (!exit);
        return new float[]{minimum, maximum};
    }


    boolean areWeBeforeBMStart(Calendar now, Calendar lowerDate) {
        return now.before(lowerDate);
    }


    boolean areWeWithinBMPeriod(Calendar now, Calendar lowerDate, Calendar upperDate) {
        return now.getTimeInMillis() >= lowerDate.getTimeInMillis() && now.before(upperDate);
    }


    boolean areWeAfterBmEnd(Calendar now, Calendar upperDate) {
        return now.getTimeInMillis() >= upperDate.getTimeInMillis();
    }


    void increaseDateWithPeriod(Calendar c, BudgetModifier.PeriodType period, int periodMultiplier) {
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
}
