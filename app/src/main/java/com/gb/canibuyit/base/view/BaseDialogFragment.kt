package com.gb.canibuyit.base.view

import android.os.Bundle
import androidx.annotation.StringRes
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.gb.canibuyit.R
import com.gb.canibuyit.util.hide
import com.gb.canibuyit.util.show
import com.gb.canibuyit.util.visible
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
        this.message.show()
        updateTitleVisibility()
        return this
    }

    fun setBigMessage(body: CharSequence): BaseDialogFragment {
        this.big_message.text = body
        this.big_message_container.show()
        updateTitleVisibility()
        return this
    }

    open fun setPositiveButton(buttonTextId: Int, listener: ((View) -> Unit)?): BaseDialogFragment {
        listener?.let { button.setOnClickListener(it) }
        button.text = getString(buttonTextId)
        button.show()
        progress_bar.hide()
        return this
    }

    protected fun updateTitleVisibility() {
        if (!dialog_title.text.isNullOrBlank()) {
            dialog_title.show()

            if (message.visible() || big_message_container.visible() || text_input.visible()) {
                title_bottom_divider.show()
            } else {
                title_bottom_divider.hide()
            }
        } else {
            dialog_title.hide()
            title_bottom_divider.hide()
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
