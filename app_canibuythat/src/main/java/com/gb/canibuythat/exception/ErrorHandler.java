package com.gb.canibuythat.exception;

import android.support.v4.app.FragmentManager;

import com.gb.canibuythat.di.Injector;
import com.gb.canibuythat.ui.LoginActivity;
import com.gb.canibuythat.ui.PromptDialog;
import com.gb.canibuythat.util.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ErrorHandler {

    private static final String TAG = "CanIBuyThat";

    @Inject
    public ErrorHandler() {
    }

    public void onError(Throwable exception) {
        if (exception instanceof DomainException) {
            PromptDialog dialog = null;
            DomainException domainException = (DomainException) exception;
            switch (domainException.getKind()) {
                case HTTP:
                    dialog = PromptDialog.newInstance("Server error " + domainException.getCode(), domainException.getMessage());
                    if (domainException.getAction() == DomainException.Action.LOGIN) {
                        dialog.setPositiveButton(android.R.string.ok, v -> LoginActivity.show(Injector.INSTANCE.getContext()));
                    }
                    break;
                case NETWORK:
                    dialog = PromptDialog.newInstance("Network error", domainException.getMessage());
                    break;
                case GENERIC:
                    dialog = PromptDialog.newInstance("Error", domainException.getMessage());
                    break;
            }

            FragmentManager fragmentManager = Injector.INSTANCE.getFragmentManager();

            if (fragmentManager != null && dialog != null) {
                dialog.show(fragmentManager, null);
            }
        }
        Logger.INSTANCE.e(TAG, exception);
    }
}
