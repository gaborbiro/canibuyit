package com.gb.canibuyit.base.error

import android.content.Context
import android.widget.Toast
import com.gb.canibuyit.base.view.PromptDialog
import com.gb.canibuyit.di.Injector
import com.gb.canibuyit.error.DomainException
import com.gb.canibuyit.feature.monzo.view.LoginActivity
import com.gb.canibuyit.util.Logger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ErrorHandler @Inject
constructor(private val appContext: Context) {

    fun onError(exception: Throwable) {
        Logger.e(TAG, exception)
        val dialog = when (exception) {
            is DomainException -> mapException(exception)
            else -> getDefaultErrorDialog(exception)
        }
        Injector.INSTANCE.fragmentManager?.let {
            dialog.show(it, null)
        }
    }

    private fun mapException(exception: DomainException): PromptDialog {
        return when (exception.kind) {
            DomainException.Kind.HTTP -> getHttpErrorDialog(exception)
            DomainException.Kind.NETWORK -> getNetworkErrorDialog(exception)
            DomainException.Kind.GENERIC -> getGenericErrorDialog(exception)
        }
    }

    private fun getHttpErrorDialog(exception: DomainException) =
        PromptDialog.messageDialog(
                title = "Server error ${exception.code}",
                message = exception.message).also {
            if (exception.action == DomainException.Action.LOGIN) {
                it.setPositiveButton(android.R.string.ok) {
                    LoginActivity.show(appContext)
                }
            }
        }

    private fun getNetworkErrorDialog(exception: DomainException) =
        PromptDialog.messageDialog(
                title = "Network error",
                message = exception.message)

    private fun getGenericErrorDialog(exception: DomainException) =
        PromptDialog.messageDialog(
                title = "Error",
                message = exception.message)

    private fun getDefaultErrorDialog(exception: Throwable) = PromptDialog.messageDialog(
            title = "Error",
            message = "${exception.message}\n\nCheck log for details")

    fun onErrorSoft(exception: Throwable) {
        when (exception) {
            is DomainException -> {
                val message = when (exception.kind) {
                    DomainException.Kind.HTTP -> "Server error ${exception.code}: ${exception.message}"
                    DomainException.Kind.NETWORK -> "Network error: ${exception.message}"
                    DomainException.Kind.GENERIC -> "Error: ${exception.message}"
                }
                Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show()
            }
        }
        Logger.e(TAG, exception)
    }
}

private const val TAG = "CanIBuyIt"
