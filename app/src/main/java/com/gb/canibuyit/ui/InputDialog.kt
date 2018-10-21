package com.gb.canibuyit.ui

import android.os.Bundle
import android.support.annotation.StringRes
import android.view.View
import com.gb.canibuyit.util.show
import kotlinx.android.synthetic.main.prompt_dialog_layout.*

class InputDialog : BaseDialogFragment() {

    @StringRes
    private var btnStringResId: Int = 0
    private var onClickListener: ((View) -> Unit)? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.getString(EXTRA_TITLE)?.let { setTitle(it) }
        arguments?.getString(EXTRA_CURRENT_INPUT)?.let { text_input.setText(it) }
        text_input.show()

        super.setPositiveButton(btnStringResId) {
            dismiss()
            onClickListener?.invoke(it)
        }
    }

    override fun setPositiveButton(buttonTextId: Int, listener: ((View) -> Unit)?): InputDialog {
        btnStringResId = buttonTextId
        onClickListener = listener
        return this
    }

    val input: String
        get() = text_input.text.toString()

    companion object {
        const val EXTRA_TITLE = "title"
        const val EXTRA_CURRENT_INPUT = "current_input"

        fun newInstance(title: String, currentInput: String?) = InputDialog().apply {
            arguments = Bundle().apply {
                putString(EXTRA_TITLE, title)
                putString(EXTRA_CURRENT_INPUT, currentInput)
            }
        }
    }
}
