package com.gb.canibuythat.ui.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.gb.canibuythat.R;
import com.gb.canibuythat.model.BudgetItem;

import butterknife.BindView;
import butterknife.ButterKnife;

public class BudgetItemViewHolder extends RecyclerView.ViewHolder {

    @BindView(R.id.name) TextView nameView;
    @BindView(R.id.amount_repetition) TextView amountRepetitionView;

    public BudgetItemViewHolder(View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);
    }

    public void bind(BudgetItem budgetItem) {
        Context context = nameView.getContext();
        if (budgetItem.getType().getSign() == BudgetItem.BUDGET_ITEM_TYPE_IN) {
            nameView.setText(context.getString(R.string.income, budgetItem.getName()));
        } else if (budgetItem.getType().getSign() == BudgetItem.BUDGET_ITEM_TYPE_NONE) {
            nameView.setText(context.getString(R.string.ignored, budgetItem.getName()));
        } else {
            nameView.setText(budgetItem.getName());
        }
        nameView.getPaint().setStrikeThruText(!budgetItem.getEnabled());
        if (budgetItem.getOccurrenceCount() == null) {
            BudgetItem.PeriodType periodType = budgetItem.getPeriodType();
            if (periodType.getStrRes() > 0) {
                String periodStr = context.getResources().getQuantityString(periodType.getStrRes(), budgetItem.getPeriodMultiplier());
                amountRepetitionView.setText(
                        context.getResources().getQuantityString(R.plurals.amount_per_period, budgetItem.getPeriodMultiplier(), budgetItem.getAmount(), budgetItem.getPeriodMultiplier(), periodStr));
            }
        } else {
            amountRepetitionView.setText(context.getResources().getQuantityString(R.plurals.amount_times, budgetItem.getOccurrenceCount(), budgetItem.getAmount(), budgetItem.getOccurrenceCount()));
        }
    }
}
