package com.gb.canibuyit.feature.spending.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentManager
import com.gb.canibuyit.base.ui.PromptDialog
import com.gb.canibuyit.feature.spending.persistence.model.ApiSpending
import com.gb.canibuyit.util.eitherOrNull
import com.gb.canibuyit.util.setSubtextWithLinks
import com.gb.canibuyit.util.show
import kotlinx.android.synthetic.main.prompt_dialog_layout.*
import java.io.Serializable

class BalanceBreakdown(val spendings: Array<Pair<ApiSpending.Category, String>>,
                       val totalIncome: String?,
                       val totalExpense: String?) : Serializable

class BalanceBreakdownDialog : PromptDialog() {

    companion object {

        fun show(breakdown: BalanceBreakdown, gm: FragmentManager) =
            BalanceBreakdownDialog().apply {
                arguments = Bundle().apply {
                    putString(EXTRA_TITLE, "Balance breakdown")
                    putSerializable(EXTRA_BREAKDOWN_KEY, breakdown)
                }
                setPositiveButton(android.R.string.ok, null).show(gm, null)
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val breakdown = arguments?.getSerializable(EXTRA_BREAKDOWN_KEY) as BalanceBreakdown
        StringBuffer().apply {
            breakdown.totalIncome?.let {
                append(it)
                appendln()
            }
            breakdown.totalExpense?.let {
                append(it)
                appendln()
            }
            Pair(breakdown.totalIncome, breakdown.totalExpense).eitherOrNull {
                appendln()
            }
            breakdown.spendings.joinTo(buffer = this, separator = "\n", transform = {
                it.second
            })
            big_message_container.show()
            big_message.setSubtextWithLinks(
                    toString(),
                    breakdown.spendings.map { it.second }.toTypedArray(),
                    breakdown.spendings.map {
                        {
                            (activity as BalanceBreakdownDialogCallback).onBalanceBreakdownItemClicked(it.first)
                        }
                    }.toTypedArray())
            updateTitleVisibility()
        }
    }
}

interface BalanceBreakdownDialogCallback {
    fun onBalanceBreakdownItemClicked(category: ApiSpending.Category)
}

private const val EXTRA_BREAKDOWN_KEY = "EXTRA_BREAKDOWN_KEY"
