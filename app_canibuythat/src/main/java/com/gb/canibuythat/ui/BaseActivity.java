package com.gb.canibuythat.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.gb.canibuythat.exception.DomainException;
import com.gb.canibuythat.util.Logger;

import butterknife.ButterKnife;
import butterknife.Unbinder;

public abstract class BaseActivity extends AppCompatActivity {

    private static final String TAG = "CanIBuyThat";

    private Unbinder unbinder;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        inject();
    }

    @Override
    public void onContentChanged() {
        super.onContentChanged();
        unbinder = ButterKnife.bind(this);
    }

    @Override
    protected void onDestroy() {
        if (unbinder != null) {
            unbinder.unbind();
        }
        super.onDestroy();
    }

    protected void showError(Throwable exception) {
        if (exception instanceof DomainException) {
            DomainException domainException = (DomainException) exception;
            switch (domainException.getKind()) {
                case HTTP:
                    Toast.makeText(this, "HTTP error (" + domainException.getCode() + "): " + domainException.getMessage(), Toast.LENGTH_LONG).show();
                    break;
                case NETWORK:
                    Toast.makeText(this, "NETWORK error" + exception.getMessage(), Toast.LENGTH_LONG).show();
                    break;
                case GENERIC:
                    Toast.makeText(this, "GENERIC error" + exception.getMessage(), Toast.LENGTH_LONG).show();
                    break;
            }
        }
        if (exception.getCause() != null) {
            Logger.e(TAG, exception.getMessage(), exception.getCause());
        }
    }

    protected abstract void inject();
}
