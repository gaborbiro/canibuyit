package com.gb.canibuyit.feature.spending.ui

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gb.canibuyit.R
import com.gb.canibuyit.feature.spending.model.Spending
import com.gb.canibuyit.util.inflate

class SpendingAdapter(private val onSpendingClickedListener: OnSpendingClickedListener) :
        RecyclerView.Adapter<SpendingViewHolder>() {

    private var spendings: List<Spending>? = null

    fun setData(spendings: List<Spending>) {
        this.spendings = spendings
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SpendingViewHolder {
        return SpendingViewHolder(parent.inflate(R.layout.list_item_spending))
    }

    override fun onBindViewHolder(holder: SpendingViewHolder, position: Int) {
        holder.bind(spendings!![position])
        holder.itemView.setOnClickListener { v ->
            onSpendingClickedListener.onSpendingClicked(spendings!![position])
        }
    }

    override fun getItemCount(): Int {
        return if (spendings != null) spendings!!.size else 0
    }

    interface OnSpendingClickedListener {
        fun onSpendingClicked(spending: Spending)
    }
}
