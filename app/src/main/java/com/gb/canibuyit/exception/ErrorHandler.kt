package com.gb.canibuyit.exception

import android.content.Context
import android.widget.Toast
import com.gb.canibuyit.di.Injector
import com.gb.canibuyit.ui.LoginActivity
import com.gb.canibuyit.ui.PromptDialog
import com.gb.canibuyit.util.Logger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ErrorHandler @Inject
constructor(private val appContext: Context) {

    fun onError(exception: Throwable) {
        var dialog: PromptDialog? = null
        when (exception) {
            is DomainException ->
                when (exception.kind) {
                    DomainException.Kind.HTTP -> {
                        dialog = PromptDialog.messageDialog("Server error " + exception.code, exception.message)
                        if (exception.action == DomainException.Action.LOGIN) {
                            dialog.setPositiveButton(android.R.string.ok) { v -> LoginActivity.show(Injector.INSTANCE.context) }
                        }
                    }
                    DomainException.Kind.NETWORK -> dialog = PromptDialog.messageDialog("Network error", exception.message)
                    DomainException.Kind.GENERIC -> dialog = PromptDialog.messageDialog("Error", exception.message)
                }
            else -> dialog = PromptDialog.messageDialog("Error", exception.message + "\n\nCheck log for details")
        }
        val fragmentManager = Injector.INSTANCE.fragmentManager

        if (fragmentManager != null) {
            dialog.show(fragmentManager, null)
        }
        Logger.e(TAG, exception)
    }

    fun onErrorSoft(exception: Throwable) {
        when (exception) {
            is DomainException -> {
                val message = when (exception.kind) {
                    DomainException.Kind.HTTP -> "Server error " + exception.code + ": " + exception.message
                    DomainException.Kind.NETWORK -> "Network error: " + exception.message
                    DomainException.Kind.GENERIC -> "Error: " + exception.message
                }
                Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show()
            }
        }
        Logger.e(TAG, exception)
    }

    companion object {
        private val TAG = "CanIBuyIt"
    }
}
