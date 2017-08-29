package com.gb.canibuythat.ui.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gb.canibuythat.R;
import com.gb.canibuythat.model.Spending;

import java.util.List;

public class SpendingAdapter extends RecyclerView.Adapter<SpendingViewHolder> {

    private List<Spending> spendings;
    private OnSpendingClickedListener onSpendingClickedListener;

    public SpendingAdapter(List<Spending> spendings, OnSpendingClickedListener onSpendingClickedListener) {
        this.spendings = spendings;
        this.onSpendingClickedListener = onSpendingClickedListener;
    }

    @Override
    public SpendingViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.list_item_spending, parent, false);
        return new SpendingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(SpendingViewHolder holder, int position) {
        holder.bind(spendings.get(position));
        holder.itemView.setOnClickListener(v -> onSpendingClickedListener.onSpendingClicked(spendings.get(position)));
    }

    @Override
    public int getItemCount() {
        return spendings.size();
    }

    public interface OnSpendingClickedListener {
        void onSpendingClicked(Spending spending);
    }
}
