package com.gb.canibuythat.ui.adapter

import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.gb.canibuythat.R
import com.gb.canibuythat.db.model.ApiSpending
import com.gb.canibuythat.model.Spending
import com.gb.canibuythat.ui.ProgressRelativeLayout
import com.gb.canibuythat.util.hide
import com.gb.canibuythat.util.show

class SpendingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val nameView: TextView = itemView.findViewById(R.id.name_lbl) as TextView
    private val iconView: ImageView = itemView.findViewById(R.id.icon) as ImageView
    private val spentView: TextView = itemView.findViewById(R.id.spent) as TextView
    private val savingView: TextView = itemView.findViewById(R.id.saving) as TextView
    private val progressView: ProgressRelativeLayout = itemView.findViewById(R.id.progress) as ProgressRelativeLayout

    fun bind(spending: Spending) {
        val context = nameView.context
        nameView.paint.isStrikeThruText = !spending.enabled
        val perCycleAmount = spending.occurrenceCount?.let {
            context.getString(R.string.spending_no_target, spending.value, context.resources.getQuantityString(R.plurals.times, it, it)) // -875.00 (once)
        } ?: let {
            context.resources.getQuantityString(R.plurals.amount_cycle, spending.cycleMultiplier,
                    spending.value, spending.cycleMultiplier, context.resources.getQuantityText(spending.cycle.strRes, spending.cycleMultiplier)) // 3045.00 per month
        }
        nameView.text = context.getString(R.string.average, spending.name, perCycleAmount) // Rent (875.0)
        spending.spent?.let { spent ->
            var cycleStr = ""
            spending.occurrenceCount?.let {
                cycleStr = context.resources.getQuantityString(R.plurals.times, it, it) // (10 times)
            } ?: let {
                cycleStr = context.resources.getQuantityString(R.plurals.period, spending.cycleMultiplier,
                        spending.cycleMultiplier, context.resources.getQuantityText(spending.cycle.strRes, spending.cycleMultiplier)) // this week
            }
            spending.target?.let {
                spentView.text = context.getString(R.string.spending, spent, it, cycleStr) // 82.79/90 this week
            } ?: let {
                spentView.text = context.getString(R.string.spending_no_target, spent, cycleStr) // 0.00 this month
            }
            spentView.show()
        } ?: let {
            spentView.hide()
        }
        if (spending.sourceData?.containsKey(ApiSpending.SOURCE_MONZO_CATEGORY) == true) {
            iconView.setImageResource(R.drawable.monzo)
            iconView.show()
        } else {
            iconView.hide()
        }
        spending.spent?.let { spent ->
            spending.target?.let {
                progressView.progress = Math.abs(spent / it).toFloat()
                progressView.mode = if (it < 0.0) ProgressRelativeLayout.Mode.MIN_LIMIT else ProgressRelativeLayout.Mode.MAX_LIMIT
            } ?: let {
                progressView.progress = Math.abs(spent / spending.value).toFloat()
                progressView.mode = ProgressRelativeLayout.Mode.DEFAULT
            }
        } ?: let {
            progressView.mode = ProgressRelativeLayout.Mode.OFF
        }
        spending.savings?.let {
            it.sumByDouble { it.amount }.let {
                savingView.text = context.getString(R.string.saving, it)
                savingView.show()
            }
        } ?: savingView.hide()
    }
}
