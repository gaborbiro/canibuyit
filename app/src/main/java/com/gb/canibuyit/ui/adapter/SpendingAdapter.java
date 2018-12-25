package com.gb.canibuyit.ui.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gb.canibuyit.R;
import com.gb.canibuyit.model.Spending;

import java.util.List;

public class SpendingAdapter extends RecyclerView.Adapter<SpendingViewHolder> {

    private List<Spending> spendings;
    private OnSpendingClickedListener onSpendingClickedListener;

    public SpendingAdapter(OnSpendingClickedListener onSpendingClickedListener) {
        this.onSpendingClickedListener = onSpendingClickedListener;
    }

    public void setData(List<Spending> spendings) {
        this.spendings = spendings;
        notifyDataSetChanged();
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
        return spendings != null ? spendings.size() : 0;
    }

    public interface OnSpendingClickedListener {
        void onSpendingClicked(Spending spending);
    }
}
