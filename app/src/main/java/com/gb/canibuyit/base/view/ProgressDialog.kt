package com.gb.canibuyit.base.view

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import kotlinx.android.synthetic.main.prompt_dialog_layout.*

class ProgressDialog : BaseDialogFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.getString(EXTRA_MESSAGE)?.let { setMessage(it) }
        button.isVisible = false
        progress_bar.isVisible = true
        isCancelable = false
    }

    companion object {
        private const val EXTRA_MESSAGE = "message"

        fun newInstance(message: String) = ProgressDialog().apply {
            arguments = Bundle().apply {
                putString(EXTRA_MESSAGE, message)
            }
        }
    }
}
