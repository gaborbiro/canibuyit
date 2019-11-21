package com.gb.canibuyit.feature.spending.ui

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.gb.canibuyit.R
import com.gb.canibuyit.feature.monzo.MONZO_CATEGORY
import com.gb.canibuyit.feature.spending.model.Saving
import com.gb.canibuyit.feature.spending.model.Spending
import java.math.BigDecimal
import com.gb.canibuyit.util.sumBy as sumByBigDecimal

class SpendingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val nameView: TextView = itemView.findViewById(R.id.name_lbl) as TextView
    private val iconView: ImageView = itemView.findViewById(R.id.icon) as ImageView
    private val spentView: TextView = itemView.findViewById(R.id.spent) as TextView
    private val savingView: TextView = itemView.findViewById(R.id.saving) as TextView
    private val progressView: ProgressRelativeLayout =
        itemView.findViewById(R.id.progress) as ProgressRelativeLayout

    fun bind(spending: Spending) {
        val context = nameView.context
        nameView.paint.isStrikeThruText = !spending.enabled
        val perCycleAmount = spending.occurrenceCount?.let {
            context.getString(R.string.spending_no_target, spending.value,
                context.resources.getQuantityString(R.plurals.times, it, it)) // -875.00 (once)
        } ?: let {
            context.resources.getQuantityString(R.plurals.amount_cycle, spending.cycleMultiplier,
                spending.value, spending.cycleMultiplier,
                context.resources.getQuantityText(spending.cycle.strRes,
                    spending.cycleMultiplier)) // 3045.00 per month
        }
        nameView.text =
            context.getString(R.string.average, spending.name, perCycleAmount) // Rent (875.0)
        spending.spent.let { spent ->
            if (spent != BigDecimal.ZERO) {
                var cycleStr = ""
                spending.occurrenceCount?.let {
                    cycleStr =
                        context.resources.getQuantityString(R.plurals.times, it, it) // (10 times)
                } ?: let {
                    cycleStr = context.resources.getQuantityString(R.plurals.period,
                        spending.cycleMultiplier,
                        spending.cycleMultiplier,
                        context.resources.getQuantityText(spending.cycle.strRes,
                            spending.cycleMultiplier)) // this week
                }
                spending.target?.let {
                    spentView.text = context.getString(R.string.spending, spent, it,
                        cycleStr) // 82.79/90 this week
                } ?: let {
                    spentView.text = context.getString(R.string.spending_no_target, spent,
                        cycleStr) // 0.00 this month
                }
                spentView.isVisible = true
            } else {
                spentView.isVisible = false
            }
        }
        if (spending.sourceData?.containsKey(MONZO_CATEGORY) == true) {
            iconView.setImageResource(R.drawable.monzo)
            iconView.isVisible = true
        } else {
            iconView.isVisible = false
        }
        spending.spent.let { spent ->
            if (spent < BigDecimal.ZERO) {
                spending.target?.let {
                    progressView.progress = (spent / it.toBigDecimal()).abs().toFloat()
                    progressView.mode =
                        if (it < 0.0) ProgressRelativeLayout.Mode.MIN_LIMIT else ProgressRelativeLayout.Mode.MAX_LIMIT
                } ?: let {
                    if (spending.value != BigDecimal.ZERO) {
                        progressView.progress = (spent / spending.value).abs().toFloat()
                        progressView.mode = ProgressRelativeLayout.Mode.DEFAULT
                    } else {
                        progressView.mode = ProgressRelativeLayout.Mode.OFF
                    }
                }
            } else {
                progressView.mode = ProgressRelativeLayout.Mode.OFF
            }
        }
        spending.savings?.let {
            it.sumByBigDecimal(Saving::amount).let { saving ->
                savingView.text = context.getString(R.string.saving, saving)
                savingView.isVisible = true
            }
        } ?: run { savingView.isVisible = false }
    }
}