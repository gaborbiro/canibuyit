package com.gb.canibuythat.ui.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gb.canibuythat.R;
import com.gb.canibuythat.model.BudgetItem;

import java.util.List;

public class BudgetItemAdapter extends RecyclerView.Adapter<BudgetItemViewHolder> {

    private List<BudgetItem> budgetItems;
    private OnBudgetItemClickedListener onBudgetItemClickedListener;

    public BudgetItemAdapter(List<BudgetItem> budgetItems, OnBudgetItemClickedListener onBudgetItemClickedListener) {
        this.budgetItems = budgetItems;
        this.onBudgetItemClickedListener = onBudgetItemClickedListener;
    }

    @Override
    public BudgetItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.list_item_budget, parent, false);
        return new BudgetItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(BudgetItemViewHolder holder, int position) {
        holder.bind(budgetItems.get(position));
        holder.itemView.setOnClickListener(v -> onBudgetItemClickedListener.onBudgetItemClicked(budgetItems.get(position)));
    }

    @Override
    public int getItemCount() {
        return budgetItems.size();
    }

    public interface OnBudgetItemClickedListener {
        void onBudgetItemClicked(BudgetItem budgetItem);
    }
}
