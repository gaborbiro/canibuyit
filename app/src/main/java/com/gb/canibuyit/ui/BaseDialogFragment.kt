package com.gb.canibuyit.ui

import android.os.Bundle
import android.support.annotation.StringRes
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.gb.canibuyit.R
import com.gb.canibuyit.util.hide
import com.gb.canibuyit.util.show
import kotlinx.android.synthetic.main.prompt_dialog_layout.*

open class BaseDialogFragment : DialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.prompt_dialog_layout, container, false)
    }

    fun setTitle(@StringRes title: Int): BaseDialogFragment {
        return setTitle(getString(title))
    }

    fun setTitle(title: String): BaseDialogFragment {
        this.title.text = title
        this.title.show()
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
        return this
    }

    fun setBigMessage(body: String): BaseDialogFragment {
        this.big_message.text = body
        this.big_message_container.show()
        return this
    }

    open fun setPositiveButton(buttonTextId: Int, listener: ((View) -> Unit)?): BaseDialogFragment {
        listener?.let { button.setOnClickListener(it) }
        button.text = getString(buttonTextId)
        button.show()
        progress_bar.hide()
        return this
    }

    override fun show(manager: FragmentManager, tag: String?) {
        val ft = manager.beginTransaction()
        ft.add(this, tag)
        ft.commitAllowingStateLoss()
    }
}
