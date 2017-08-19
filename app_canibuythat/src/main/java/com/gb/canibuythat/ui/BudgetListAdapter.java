package com.gb.canibuythat.ui;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gb.canibuythat.R;
import com.gb.canibuythat.model.BudgetItem;
import com.gb.canibuythat.provider.Contract;
import com.gb.canibuythat.ui.dragndroplist.DragNDropCursorAdapter;

class BudgetListAdapter extends DragNDropCursorAdapter
        implements SimpleCursorAdapter.ViewBinder {

    BudgetListAdapter(Context context, Cursor c) {
        super(context, R.layout.budget_list_item, c,
                new String[]{Contract.BudgetItem.NAME, Contract.BudgetItem.AMOUNT},
                new int[]{R.id.name, R.id.amount_repetition}, R.id.drag_n_drop_handle);
        setViewBinder(this);
    }

    @Override
    public View getView(int position, View view, ViewGroup group) {
        return super.getView(position, view, group);
    }

    @Override
    public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
        if (view.getId() == R.id.name) {
            String name = cursor.getString(cursor.getColumnIndex(Contract.BudgetItem.NAME));
            BudgetItem.BudgetItemType type = BudgetItem.BudgetItemType.valueOf(cursor.getString(cursor.getColumnIndex(Contract.BudgetItem.TYPE)));
            TextView nameView = (TextView) view;
            if (type.getSign() == BudgetItem.BUDGET_ITEM_TYPE_IN) {
                nameView.setText(mContext.getString(R.string.income, name));
            } else {
                nameView.setText(name);
            }
            boolean enabled = cursor.getInt(cursor.getColumnIndex(Contract.BudgetItem.ENABLED)) > 0;
            nameView.getPaint().setStrikeThruText(!enabled);
        } else if (view.getId() == R.id.amount_repetition) {
            TextView amountRepetitionView = (TextView) view;
            int occurrenceCount = cursor.getInt(cursor.getColumnIndex(Contract.BudgetItem.OCCURRENCE_COUNT));
            float amount = cursor.getFloat(cursor.getColumnIndex(Contract.BudgetItem.AMOUNT));

            if (occurrenceCount < 1) {
                BudgetItem.PeriodType period = BudgetItem.PeriodType.valueOf(
                        cursor.getString(cursor.getColumnIndex(Contract.BudgetItem.PERIOD_TYPE)));
                int periodMultiplier = cursor.getInt(cursor.getColumnIndex(Contract.BudgetItem.PERIOD_MULTIPLIER));
                if (period.strRes > 0) {
                    String periodStr = mContext.getResources().getQuantityString(period.strRes, periodMultiplier);
                    amountRepetitionView.setText(
                            mContext.getResources().getQuantityString(R.plurals.amount_per_period, periodMultiplier, amount, periodMultiplier, periodStr));
                }
            } else {
                amountRepetitionView.setText(mContext.getResources().getQuantityString(R.plurals.amount_times, occurrenceCount, amount, occurrenceCount));
            }
        }
        return true;
    }
}
