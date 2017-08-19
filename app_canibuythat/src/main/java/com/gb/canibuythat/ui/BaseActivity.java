package com.gb.canibuythat.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.gb.canibuythat.exception.DomainException;

import butterknife.ButterKnife;
import butterknife.Unbinder;

public abstract class BaseActivity extends AppCompatActivity {

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

    protected void showError(DomainException exception) {
        switch (exception.getKind()) {
            case HTTP:
                Toast.makeText(this, "HTTP error (" + exception.getCode() + "): " + exception.getMessage(), Toast.LENGTH_LONG);
                break;
            case NETWORK:
                Toast.makeText(this, "NETWORK error" + exception.getMessage(), Toast.LENGTH_LONG);
                break;
            case GENERIC:
                Toast.makeText(this, "GENERIC error" + exception.getMessage(), Toast.LENGTH_LONG);
                break;
        }
    }

    protected abstract void inject();
}
