package com.gb.canibuythat.ui

import android.os.Bundle
import android.view.View
import com.gb.canibuythat.util.hide
import com.gb.canibuythat.util.show
import kotlinx.android.synthetic.main.prompt_dialog_layout.*

class ProgressDialog : BaseDialogFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.getString(EXTRA_MESSAGE)?.let { setMessage(it) }
        button.hide()
        progress_bar.show()
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
