package com.gb.canibuythat.ui;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.View;
import android.widget.TextView;

import com.gb.canibuythat.R;
import com.gb.canibuythat.model.BudgetItem;
import com.gb.canibuythat.provider.Contract;

public class BudgetItemListAdapter extends SimpleCursorAdapter {

    public BudgetItemListAdapter(Context context, Cursor c) {
        super(context, R.layout.budget_item_list_item, c, new String[0], null, 0);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        TextView amountView = (TextView) view.findViewById(R.id.name);
        String name = cursor.getString(cursor.getColumnIndex(Contract.BudgetItem.NAME));
        BudgetItem.BudgetItemType type = BudgetItem.BudgetItemType.valueOf(
                cursor.getString(cursor.getColumnIndex(Contract.BudgetItem.TYPE)));

        if (type.getSign() == BudgetItem.BUDGET_ITEM_TYPE_IN) {
            amountView.setText(mContext.getString(R.string.income, name));
        } else {
            amountView.setText(name);
        }

        TextView amountRepetitionView =
                (TextView) view.findViewById(R.id.amount_repetition);
        int occurrenceCount = cursor.getInt(
                cursor.getColumnIndex(Contract.BudgetItem.OCCURRENCE_COUNT));
        float amount = cursor.getFloat(cursor.getColumnIndex(Contract.BudgetItem.AMOUNT));

        if (occurrenceCount < 1) {
            BudgetItem.PeriodType period = BudgetItem.PeriodType.valueOf(cursor.getString(
                    cursor.getColumnIndex(Contract.BudgetItem.PERIOD_TYPE)));
            int periodMultiplier = cursor.getInt(
                    cursor.getColumnIndex(Contract.BudgetItem.PERIOD_MULTIPLIER));
            String periodStr =
                    mContext.getResources().getQuantityString(period.strRes, periodMultiplier);
            amountRepetitionView.setText(
                    mContext.getResources().getQuantityString(R.plurals.amount_per_period,
                            periodMultiplier, amount, periodMultiplier, periodStr));
        } else {
            amountRepetitionView.setText(
                    mContext.getResources().getQuantityString(R.plurals.amount_times,
                            occurrenceCount, amount, occurrenceCount));
        }
    }
}
