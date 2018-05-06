package com.gb.canibuythat.ui

import android.os.Bundle
import android.support.annotation.StringRes
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.gb.canibuythat.R
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
        this.title.visibility = View.VISIBLE
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
        this.message.visibility = View.VISIBLE
        return this
    }

    fun setBigMessage(body: String): BaseDialogFragment {
        this.big_message.text = body
        this.big_message_container.visibility = View.VISIBLE
        return this
    }

    open fun setPositiveButton(buttonTextId: Int, listener: ((View) -> Unit)?): BaseDialogFragment {
        listener?.let { button.setOnClickListener(it) }
        button.text = getString(buttonTextId)
        button.visibility = View.VISIBLE
        progress_bar.visibility = View.GONE
        return this
    }

    override fun show(manager: FragmentManager, tag: String?) {
        val ft = manager.beginTransaction()
        ft.add(this, tag)
        ft.commitAllowingStateLoss()
    }
}
