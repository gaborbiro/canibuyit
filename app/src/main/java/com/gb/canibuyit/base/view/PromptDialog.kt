package com.gb.canibuyit.base.view

import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes

open class PromptDialog : BaseDialogFragment() {

    @StringRes
    private var btnStringResId = android.R.string.ok
    private var onClickListener: ((View) -> Unit)? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.getString(EXTRA_TITLE)?.let(this@PromptDialog::setTitle)
        arguments?.getString(EXTRA_MESSAGE)?.let(this@PromptDialog::setMessage)
        arguments?.getCharSequence(
            EXTRA_BIG_MESSAGE)?.let(this@PromptDialog::setBigMessage)

        super.setPositiveButton(btnStringResId) {
            dismiss()
            onClickListener?.invoke(it)
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun setPositiveButton(buttonTextId: Int, listener: ((View) -> Unit)?): PromptDialog {
        btnStringResId = buttonTextId
        onClickListener = listener
        return this
    }

    companion object {
        const val EXTRA_TITLE = "title"
        const val EXTRA_MESSAGE = "message"
        const val EXTRA_BIG_MESSAGE = "big_message"

        fun messageDialog(title: String, message: String?) =
            PromptDialog().apply {
                arguments = Bundle().apply {
                    putString(EXTRA_TITLE, title)
                    putString(EXTRA_MESSAGE, message)
                }
            }

        fun bigMessageDialog(title: String, message: String?) =
            PromptDialog().apply {
                arguments = Bundle().apply {
                    putString(EXTRA_TITLE, title)
                    putCharSequence(
                        EXTRA_BIG_MESSAGE, message)
                }
            }

        fun bigMessageDialog(title: String, message: SpannableStringBuilder) =
            PromptDialog().apply {
                arguments = Bundle().apply {
                    putString(EXTRA_TITLE, title)
                    putCharSequence(
                        EXTRA_BIG_MESSAGE, message)
                }
            }
    }
}
