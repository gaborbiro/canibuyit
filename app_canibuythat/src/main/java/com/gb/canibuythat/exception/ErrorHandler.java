package com.gb.canibuythat.exception;

import android.content.Context;
import android.support.v4.app.FragmentManager;
import android.widget.Toast;

import com.gb.canibuythat.di.Injector;
import com.gb.canibuythat.ui.LoginActivity;
import com.gb.canibuythat.ui.PromptDialog;
import com.gb.canibuythat.util.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ErrorHandler {

    private static final String TAG = "CanIBuyThat";

    private Context appContext;

    @Inject
    public ErrorHandler(Context appContext) {
        this.appContext = appContext;
    }

    public void onError(Throwable exception) {
        PromptDialog dialog = null;
        if (exception instanceof DomainException) {
            DomainException domainException = (DomainException) exception;
            switch (domainException.getKind()) {
                case HTTP:
                    dialog = PromptDialog.messageDialog("Server error " + domainException.getCode(), domainException.getMessage());
                    if (domainException.getAction() == DomainException.Action.LOGIN) {
                        dialog.setPositiveButton(android.R.string.ok, v -> LoginActivity.show(Injector.INSTANCE.getContext()));
                    }
                    break;
                case NETWORK:
                    dialog = PromptDialog.messageDialog("Network error", domainException.getMessage());
                    break;
                case GENERIC:
                    dialog = PromptDialog.messageDialog("Error", domainException.getMessage());
                    break;
            }
        } else {
            dialog = PromptDialog.messageDialog("Error", exception.getMessage() + "\n\nCheck log for details");
        }
        FragmentManager fragmentManager = Injector.INSTANCE.getFragmentManager();

        if (fragmentManager != null && dialog != null) {
            dialog.show(fragmentManager, null);
        }
        Logger.INSTANCE.e(TAG, exception);
    }

    public void onErrorSoft(Throwable exception) {
        if (exception instanceof DomainException) {
            DomainException domainException = (DomainException) exception;
            String message = null;
            switch (domainException.getKind()) {
                case HTTP:
                    message = "Server error " + domainException.getCode() + ": " + domainException.getMessage();
                    break;
                case NETWORK:
                    message = "Network error: " + domainException.getMessage();
                    break;
                case GENERIC:
                    message = "Error: " + domainException.getMessage();
                    break;
            }
            Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show();
        }
        Logger.INSTANCE.e(TAG, exception);
    }
}
