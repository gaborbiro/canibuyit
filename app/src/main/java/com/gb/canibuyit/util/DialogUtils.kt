package com.gb.canibuyit.util

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface

object DialogUtils {

    /**
     * @param onSave   will be run if user selects Yes
     * @param onFinish will be run if user selects Yes and onSave returns true OR if
     * user selects Discard (in which case onSave is ignored)
     */
    fun getSaveOrDiscardDialog(context: Context?, message: String?, onSave: Executable): AlertDialog {
        return getDialog(
                context,
                title = "Save changes?",
                message = message,
                positiveButton = "Save",
                onPositiveButtonClicked = ClickRunner.ConditionalTaskBuilder(onSave).build(),
                negativeButton = "Discard",
                onNegativeButtonClicked = ClickRunner.Builder().build(),
                fuckOffButton = android.R.string.cancel,
                onFuckOffButtonClicked = null)
    }

    /**
     * @param onSave   will be run if user selects Yes
     * @param onFinish will be run if user selects Yes and onSave returns true OR if
     * user selects Discard (in which case onSave is ignored)
     */
    fun getSaveOrDiscardDialog(context: Context?, message: String?, onSave: Executable, onFinish: () -> Unit): AlertDialog {
        return getDialog(
                context,
                title = "Save changes?",
                message = message,
                positiveButton = "Save",
                onPositiveButtonClicked = ClickRunner.ConditionalTaskBuilder(onSave).onSuccess(onFinish).build(),
                negativeButton = "Discard",
                onNegativeButtonClicked = ClickRunner.Builder().setOnDiscard(onFinish).build(),
                fuckOffButton = android.R.string.cancel,
                onFuckOffButtonClicked = null)
    }

    /**
     * @param title                   CharSequence or resource id
     * @param positiveButton          CharSequence or resource id
     * @param onPositiveButtonClicked
     * @param negativeButton          CharSequence or resource id
     * @param onNegativeButtonClicked
     * @param fuckOffButton           CharSequence or resource id
     * @param onFuckOffButtonClicked
     */
    fun getDialog(context: Context?, title: Any?, message: Any?, positiveButton: Any?, onPositiveButtonClicked: ClickRunner,
                  negativeButton: Any?, onNegativeButtonClicked: ClickRunner, fuckOffButton: Any?, onFuckOffButtonClicked: ClickRunner?): AlertDialog {
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
        if (fuckOffButton != null) {
            if (fuckOffButton is CharSequence) {
                builder.setNeutralButton(fuckOffButton as CharSequence?, onFuckOffButtonClicked)
            } else if (fuckOffButton is Int) {
                builder.setNeutralButton(fuckOffButton, onFuckOffButtonClicked)
            } else {
                throw IllegalArgumentException("Wrong fuck off button text type in DialogUtils.getDialog(...)")
            }
        }
        return builder.create()
    }

    abstract class Executable {
        abstract fun run(): Boolean
    }

    class ClickRunner : DialogInterface.OnClickListener {

        private val conditionalTask: Executable?
        private val onSuccess: (() -> Unit)?
        private val onFail: (() -> Unit)?

        internal constructor(builder: Builder) {
            conditionalTask = null
            onSuccess = builder.onDiscard
            onFail = null
        }

        internal constructor(taskBuilder: ConditionalTaskBuilder) {
            conditionalTask = taskBuilder.conditionalTask
            onSuccess = taskBuilder.onSuccess
            onFail = taskBuilder.onFail
        }

        override fun onClick(dialog: DialogInterface, which: Int) {
            val result = conditionalTask == null || conditionalTask.run()

            if (result && onSuccess != null) {
                try {
                    onSuccess.invoke()
                } catch (t: Throwable) {
                    Logger.d(ClickRunner::class.java.name, t)
                }
            } else if (onFail != null) {
                try {
                    onFail.invoke()
                } catch (t: Throwable) {
                    Logger.d(ClickRunner::class.java.name, t)
                }
            }
        }

        internal class Builder {
            var onDiscard: (() -> Unit)? = null

            fun setOnDiscard(onDiscard: () -> Unit): Builder {
                this.onDiscard = onDiscard
                return this
            }

            fun build(): ClickRunner {
                return ClickRunner(this)
            }
        }

        internal class ConditionalTaskBuilder(val conditionalTask: Executable) {
            var onSuccess: (() -> Unit)? = null
            var onFail: (() -> Unit)? = null

            fun onSuccess(onSuccess: () -> Unit): ConditionalTaskBuilder {
                this.onSuccess = onSuccess
                return this
            }

            fun onFail(onFail: () -> Unit): ConditionalTaskBuilder {
                this.onFail = onFail
                return this
            }

            fun build(): ClickRunner {
                return ClickRunner(this)
            }
        }
    }
}
