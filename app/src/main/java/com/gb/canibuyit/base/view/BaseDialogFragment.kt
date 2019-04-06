package com.gb.canibuyit.base.view

import android.os.Bundle
import androidx.annotation.StringRes
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.gb.canibuyit.R
import kotlinx.android.synthetic.main.prompt_dialog_layout.*

open class BaseDialogFragment : DialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.prompt_dialog_layout, container, false)
    }

    fun setTitle(@StringRes title: Int): BaseDialogFragment {
        return setTitle(getString(title))
    }

    fun setTitle(title: String): BaseDialogFragment {
        dialog_title.text = title
        updateTitleVisibility()
        return this
    }

    fun setMessage(@StringRes message: Int): BaseDialogFragment {
        return setMessage(getString(message))
    }

    fun setBigMessage(@StringRes message: Int): BaseDialogFragment {
        return setBigMessage(getString(message))
    }

    fun setMessage(body: String): BaseDialogFragment {
        this.message.text = body
        this.message.isVisible = true
        updateTitleVisibility()
        return this
    }

    fun setBigMessage(body: CharSequence): BaseDialogFragment {
        this.big_message.text = body
        this.big_message_container.isVisible = true
        updateTitleVisibility()
        return this
    }

    open fun setPositiveButton(buttonTextId: Int, listener: ((View) -> Unit)?): BaseDialogFragment {
        listener?.let { button.setOnClickListener(it) }
        button.text = getString(buttonTextId)
        button.isVisible = true
        progress_bar.isVisible = false
        return this
    }

    protected fun updateTitleVisibility() {
        if (!dialog_title.text.isNullOrBlank()) {
            dialog_title.isVisible = true

            title_bottom_divider.isVisible = message.isVisible || big_message_container.isVisible || text_input.isVisible
        } else {
            dialog_title.isVisible = false
            title_bottom_divider.isVisible = false
        }
    }

    override fun show(manager: FragmentManager, tag: String?) {
        val ft = manager.beginTransaction()
        ft.add(this, tag)
        ft.commitAllowingStateLoss()
    }

    fun hide(manager: FragmentManager) {
        manager.findFragmentByTag(tag)?.let {
            manager.beginTransaction().remove(it).commitAllowingStateLoss()
        }
    }
}
