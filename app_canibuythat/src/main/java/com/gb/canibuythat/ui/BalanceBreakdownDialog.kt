package com.gb.canibuythat.ui

import android.os.Bundle
import android.support.v4.app.FragmentManager
import android.view.View
import com.gb.canibuythat.db.model.ApiSpending
import com.gb.canibuythat.util.setTextWithLinks
import kotlinx.android.synthetic.main.prompt_dialog_layout.*
import java.io.Serializable

class BalanceBreakdown(val spendings: Array<Pair<ApiSpending.Category, String>>,
                       val totalIncome: String?,
                       val totalExpense: String?) : Serializable

class BalanceBreakdownDialog : PromptDialog() {

    companion object {
        private const val EXTRA_BREAKDOWN_KEY = "EXTRA_BREAKDOWN_KEY"

        fun show(breakdown: BalanceBreakdown, gm: FragmentManager) = BalanceBreakdownDialog().apply {
            arguments = Bundle().apply {
                putString(PromptDialog.EXTRA_TITLE, "Balance breakdown")
                putSerializable(EXTRA_BREAKDOWN_KEY, breakdown)
            }
            setPositiveButton(android.R.string.ok, null).show(gm, null)
        }


        interface Callback {
            fun onBalanceBreakdownItemClicked(category: ApiSpending.Category)
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
            breakdown.spendings.joinTo(buffer = this, separator = "\n", transform = {
                it.second
            })
            big_message_container.visibility = View.VISIBLE
            big_message.setTextWithLinks(
                    toString(),
                    breakdown.spendings.map { it.second }.toTypedArray(),
                    breakdown.spendings.map { { (activity as Callback).onBalanceBreakdownItemClicked(it.first) } }.toTypedArray())
        }
    }
}