package com.gb.canibuythat.ui.adapter

import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.gb.canibuythat.R
import com.gb.canibuythat.model.Spending
import com.gb.canibuythat.ui.ProgressRelativeLayout

class SpendingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    val nameView: TextView = itemView.findViewById(R.id.name) as TextView
    val iconView: ImageView = itemView.findViewById(R.id.icon) as ImageView
    val detailView: TextView = itemView.findViewById(R.id.details) as TextView
    val progressView: ProgressRelativeLayout = itemView.findViewById(R.id.progress) as ProgressRelativeLayout

    fun bind(spending: Spending) {
        val context = nameView.context
        if (spending.average!! > 0) {
            nameView.text = context.getString(R.string.income, spending.name)
        } else {
            nameView.text = spending.name
        }
        nameView.paint.isStrikeThruText = !spending.enabled
        if (spending.spent != null) {
            var periodStr = ""
            if (spending.occurrenceCount == null) {
                val period = spending.period
                if (period!!.strRes > 0) {
                    periodStr = context.resources.getQuantityString(R.plurals.period, spending.periodMultiplier!!, spending.periodMultiplier!!, context.getString(period.strRes))
                }
            } else {
                periodStr = context.resources.getQuantityString(R.plurals.times, spending.occurrenceCount!!, spending.occurrenceCount!!)
            }

            if (spending.target != null) {
                detailView.text = context.getString(R.string.spending, Math.abs(spending.spent!!), spending.target, periodStr)
            } else {
                detailView.text = context.getString(R.string.spending_no_target, Math.abs(spending.spent!!), periodStr)
            }
        } else {
            detailView.text = "?"
        }
        if (spending.sourceData.containsKey(Spending.SOURCE_MONZO_CATEGORY)) {
            iconView.setImageResource(R.drawable.monzo)
            iconView.visibility = View.VISIBLE
        } else {
            iconView.visibility = View.GONE
        }
        if (spending.spent != null && spending.target != null) {
            val progress: Float = (Math.abs(spending.spent!!) / spending.target!!).toFloat()
            progressView.progress = progress
        } else {
            progressView.progress = 0f
        }
    }
}