package com.gb.canibuythat.ui.adapter;

import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gb.canibuythat.R;
import com.gb.canibuythat.model.Spending;

import java.util.ArrayList;
import java.util.List;

public class SpendingAdapter extends RecyclerView.Adapter<SpendingViewHolder> {

    private List<Spending> spendings;
    private SpendingListDiffCallback callback = new SpendingListDiffCallback();
    private OnSpendingClickedListener onSpendingClickedListener;

    public SpendingAdapter(OnSpendingClickedListener onSpendingClickedListener) {
        this.onSpendingClickedListener = onSpendingClickedListener;
    }

    public void setData(List<Spending> spendings) {
//        if (this.spendings == null || this.spendings.size() == 0) {
            this.spendings = spendings;
            notifyDataSetChanged();
//        } else {
//            this.spendings = spendings;
//            callback.setNewSpendings(spendings);
//            DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(callback);
//            diffResult.dispatchUpdatesTo(this);
//        }
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

    private class SpendingListDiffCallback extends DiffUtil.Callback {

        private List<Spending> oldSpendings = new ArrayList<>();
        private List<Spending> newSpendings;

        void setNewSpendings(List<Spending> newSpendings) {
            this.oldSpendings = this.newSpendings;
            this.newSpendings = newSpendings;
        }

        @Override
        public int getOldListSize() {
            return oldSpendings != null ? oldSpendings.size() : 0;
        }

        @Override
        public int getNewListSize() {
            return newSpendings.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return oldSpendings.get(oldItemPosition).getId() == newSpendings.get(newItemPosition).getId();
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            return oldSpendings.get(oldItemPosition).equals(newSpendings.get(newItemPosition));
        }
    }
}
