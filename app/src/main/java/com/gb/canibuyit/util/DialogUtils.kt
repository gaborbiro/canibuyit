package com.gb.canibuyit.util

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface

object DialogUtils {

    /**
     * @param negativeTask will be run if user taps the Discard button
     */
    fun getDiscardDialog(context: Context, message: String, negativeTask: () -> Unit): AlertDialog {
        return getDialog(
                context,
                title = "Cannot save changes",
                message = message,
                negativeButton = "Discard",
                onNegativeButtonClicked = ClickRunner(negativeTask),
                neutralButton = android.R.string.cancel,
                onNeutralButtonClicked = null)
    }

    /**
     * @param positiveTask   will be run if user taps Save
     */
    fun getSaveDialog(context: Context, message: String?, positiveTask: () -> Unit): AlertDialog {
        return getDialog(
                context,
                title = "Save changes?",
                message = message,
                positiveButton = "Save",
                onPositiveButtonClicked = ClickRunner(positiveTask),
                neutralButton = android.R.string.cancel,
                onNeutralButtonClicked = null)
    }

    /**
     * @param positiveConditionalTask   will be run if user taps Save
     * @param onFinish will be run if user selects Save and positiveConditionalTask returns true OR if
     * user selects Discard (in which case positiveConditionalTask is ignored)
     */
    fun getSaveOrDiscardDialog(context: Context, message: String?, positiveConditionalTask: () -> Boolean, onFinish: () -> Unit): AlertDialog {
        return getDialog(
                context,
                title = "Save changes?",
                message = message,
                positiveButton = "Save",
                onPositiveButtonClicked = ClickRunner(onFinish, positiveConditionalTask),
                negativeButton = "Discard",
                onNegativeButtonClicked = ClickRunner(onFinish),
                neutralButton = android.R.string.cancel,
                onNeutralButtonClicked = null)
    }

    /**
     * @param title                   CharSequence or resource id
     * @param positiveButton          CharSequence or resource id
     * @param onPositiveButtonClicked
     * @param negativeButton          CharSequence or resource id
     * @param onNegativeButtonClicked
     * @param neutralButton           CharSequence or resource id
     * @param onNeutralButtonClicked
     */
    fun getDialog(context: Context, title: Any?, message: Any?, positiveButton: Any? = null, onPositiveButtonClicked: ClickRunner? = null,
                  negativeButton: Any? = null, onNegativeButtonClicked: ClickRunner? = null, neutralButton: Any?, onNeutralButtonClicked: ClickRunner?): AlertDialog {
        val builder = AlertDialog.Builder(context)
        if (title != null) {
            if (title is CharSequence) {
                builder.setTitle(title as CharSequence?)
            } else if (title is Int) {
                builder.setTitle(title)
            } else {
                throw IllegalArgumentException("Wrong title type in DialogUtils.getDialog(...)")
            }
        }
        if (message != null) {
            if (message is CharSequence) {
                builder.setMessage(message as CharSequence?)
            } else if (title is Int) {
                builder.setMessage(message as Int)
            } else {
                throw IllegalArgumentException("Wrong title type in DialogUtils.getDialog(...)")
            }
        }
        if (positiveButton != null) {
            if (positiveButton is CharSequence) {
                builder.setPositiveButton(positiveButton as CharSequence?, onPositiveButtonClicked)
            } else if (positiveButton is Int) {
                builder.setPositiveButton(positiveButton, onPositiveButtonClicked)
            } else {
                throw IllegalArgumentException("Wrong positive button text type in DialogUtils.getDialog(...)")
            }
        }
        if (negativeButton != null) {
            if (negativeButton is CharSequence) {
                builder.setNegativeButton(negativeButton as CharSequence?, onNegativeButtonClicked)
            } else if (negativeButton is Int) {
                builder.setNegativeButton(negativeButton, onNegativeButtonClicked)
            } else {
                throw IllegalArgumentException("Wrong negative button text type in DialogUtils.getDialog(...)")
            }
        }
        if (neutralButton != null) {
            if (neutralButton is CharSequence) {
                builder.setNeutralButton(neutralButton as CharSequence?, onNeutralButtonClicked)
            } else if (neutralButton is Int) {
                builder.setNeutralButton(neutralButton, onNeutralButtonClicked)
            } else {
                throw IllegalArgumentException("Wrong fuck off button text type in DialogUtils.getDialog(...)")
            }
        }
        return builder.create()
    }

    class ClickRunner @JvmOverloads constructor(private var onSuccess: (() -> Unit)? = null,
                                                private var conditionalTask: (() -> Boolean)? = null,
                                                private var onFail: (() -> Unit)? = null) : DialogInterface.OnClickListener {

        override fun onClick(dialog: DialogInterface, which: Int) {
            conditionalTask?.let {
                if (it.invoke()) onSuccess?.invoke() else onFail?.invoke()
            } ?: onSuccess?.invoke()
        }
    }
}
