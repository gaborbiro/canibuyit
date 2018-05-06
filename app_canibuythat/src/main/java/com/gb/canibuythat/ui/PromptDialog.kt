package com.gb.canibuythat.ui

import android.os.Bundle
import android.support.annotation.StringRes
import android.view.View

open class PromptDialog : BaseDialogFragment() {

    @StringRes
    private var btnStringResId = android.R.string.ok
    private var onClickListener: ((View) -> Unit)? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.getString(EXTRA_TITLE)?.let { setTitle(it) }
        arguments?.getString(EXTRA_MESSAGE)?.let { setMessage(it) }
        arguments?.getString(EXTRA_BIG_MESSAGE)?.let { setBigMessage(it) }

        super.setPositiveButton(btnStringResId) {
            dismiss()
            onClickListener?.invoke(it)
        }
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

        fun messageDialog(title: String, message: String?) = PromptDialog().apply {
            arguments = Bundle().apply {
                putString(EXTRA_TITLE, title)
                putString(EXTRA_MESSAGE, message)
            }
        }

        fun bigMessageDialog(title: String, message: String?): PromptDialog {
            val promptDialog = PromptDialog()

            val args = Bundle()
            args.putString(EXTRA_TITLE, title)
            args.putString(EXTRA_BIG_MESSAGE, message)
            promptDialog.arguments = args

            return promptDialog
        }
    }
}