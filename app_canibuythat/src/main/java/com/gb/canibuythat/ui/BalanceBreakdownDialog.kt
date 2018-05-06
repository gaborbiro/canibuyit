package com.gb.canibuythat.ui

import android.os.Bundle
import android.view.View
import com.gb.canibuythat.db.model.ApiSpending
import com.gb.canibuythat.util.setTextWithLinks
import kotlinx.android.synthetic.main.prompt_dialog_layout.*

class BalanceBreakdownDialog : PromptDialog() {

    companion object {
        interface Callback {
            fun onBalanceBreakdownItemClicked(category: ApiSpending.Category)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val breakdown = arguments?.getSerializable("breakdown") as Array<Pair<ApiSpending.Category, String>>
        StringBuffer().apply {
            breakdown.joinTo(buffer = this, separator = "\n", transform = {
                it.second
            })
            big_message.visibility = View.VISIBLE
            big_message.setTextWithLinks(
                    toString(),
                    breakdown.map { it.second }.toTypedArray(),
                    breakdown.map { { (activity as Callback).onBalanceBreakdownItemClicked(it.first) } }.toTypedArray())
        }
    }
}