package com.gb.canibuyit.ui

import android.os.Bundle
import android.view.View
import com.gb.canibuyit.util.hide
import com.gb.canibuyit.util.show
import kotlinx.android.synthetic.main.prompt_dialog_layout.*

class ProgressDialog : BaseDialogFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.getString(EXTRA_MESSAGE)?.let { setMessage(it) }
        button.hide()
        progress_bar.show()
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
