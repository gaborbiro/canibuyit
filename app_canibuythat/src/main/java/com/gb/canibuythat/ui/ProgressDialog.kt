package com.gb.canibuythat.ui

import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.prompt_dialog_layout.*

class ProgressDialog : BaseDialogFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.getString(EXTRA_MESSAGE)?.let { setMessage(it) }
        button.visibility = View.GONE
        progress_bar.visibility = View.VISIBLE
    }

    companion object {
        const val EXTRA_MESSAGE = "message"

        fun newInstance(message: String) = ProgressDialog().apply {
            arguments = Bundle().apply {
                putString(EXTRA_MESSAGE, message)
            }
        }
    }
}
