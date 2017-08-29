package com.gb.canibuythat.ui.adapter

import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.gb.canibuythat.R
import com.gb.canibuythat.model.Spending

class SpendingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    val nameView: TextView = itemView.findViewById(R.id.name) as TextView
    val iconView: ImageView = itemView.findViewById(R.id.icon) as ImageView
    val amountRepetitionSpentView: TextView = itemView.findViewById(R.id.amount_repetition_spent) as TextView

    fun bind(spending: Spending) {
        val context = nameView.context
        if (spending.amount!! > 0) {
            nameView.text = context.getString(R.string.income, spending.name)
        } else if (!spending.enabled) {
            nameView.text = context.getString(R.string.ignored, spending.name)
        } else {
            nameView.text = spending.name
        }
        nameView.paint.isStrikeThruText = !spending.enabled
        if (spending.occurrenceCount == null) {
            val period = spending.period
            if (period!!.strRes > 0) {
                val periodStr = context.resources.getQuantityString(period.strRes, spending.periodMultiplier!!)
                amountRepetitionSpentView.text = context.resources.getQuantityString(R.plurals.amount_per_period,
                        spending.periodMultiplier!!,
                        Math.abs(spending.amount!!),
                        spending.periodMultiplier, periodStr,
                        Math.abs(spending.spent!!))
            }
        } else {
            amountRepetitionSpentView.text = context.resources.getQuantityString(R.plurals.amount_times,
                    spending.occurrenceCount!!,
                    Math.abs(spending.amount!!),
                    spending.occurrenceCount,
                    Math.abs(spending.spent!!))
        }
        if (spending.sourceData.containsKey(Spending.SOURCE_MONZO_CATEGORY)) {
            iconView.setImageResource(R.drawable.monzo)
            iconView.visibility = View.VISIBLE
        } else {
            iconView.visibility = View.GONE
        }
    }
}
