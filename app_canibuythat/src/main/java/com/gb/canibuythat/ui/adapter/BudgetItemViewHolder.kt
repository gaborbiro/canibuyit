package com.gb.canibuythat.ui.adapter

import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.gb.canibuythat.R
import com.gb.canibuythat.model.BudgetItem

class BudgetItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    val nameView: TextView = itemView.findViewById(R.id.name) as TextView
    val iconView: ImageView = itemView.findViewById(R.id.icon) as ImageView
    val amountRepetitionSpentView: TextView = itemView.findViewById(R.id.amount_repetition_spent) as TextView

    fun bind(budgetItem: BudgetItem) {
        val context = nameView.context
        if (budgetItem.amount!! > 0) {
            nameView.text = context.getString(R.string.income, budgetItem.name)
        } else if (!budgetItem.enabled) {
            nameView.text = context.getString(R.string.ignored, budgetItem.name)
        } else {
            nameView.text = budgetItem.name
        }
        nameView.paint.isStrikeThruText = !budgetItem.enabled
        if (budgetItem.occurrenceCount == null) {
            val periodType = budgetItem.periodType
            if (periodType!!.strRes > 0) {
                val periodStr = context.resources.getQuantityString(periodType.strRes, budgetItem.periodMultiplier!!)
                amountRepetitionSpentView.text = context.resources.getQuantityString(R.plurals.amount_per_period,
                        budgetItem.periodMultiplier!!,
                        Math.abs(budgetItem.amount!!),
                        budgetItem.periodMultiplier, periodStr,
                        Math.abs(budgetItem.spent!!))
            }
        } else {
            amountRepetitionSpentView.text = context.resources.getQuantityString(R.plurals.amount_times,
                    budgetItem.occurrenceCount!!,
                    Math.abs(budgetItem.amount!!),
                    budgetItem.occurrenceCount,
                    Math.abs(budgetItem.spent!!))
        }
        if (budgetItem.sourceData.containsKey(BudgetItem.SOURCE_MONZO_CATEGORY)) {
            iconView.setImageResource(R.drawable.monzo)
            iconView.visibility = View.VISIBLE
        } else {
            iconView.visibility = View.GONE
        }
    }
}
