package com.gb.canibuythat.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.gb.canibuythat.di.Injector;
import com.gb.canibuythat.exception.ErrorHandler;
import com.gb.canibuythat.exception.FragmentManagerSource;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.Unbinder;

public abstract class BaseActivity extends AppCompatActivity implements FragmentManagerSource {

    @Inject ErrorHandler errorHandler;

    private Unbinder unbinder;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        inject();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Injector.INSTANCE.registerDialogHandler(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Injector.INSTANCE.unregisterDialogHandler(this);
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

    protected void onError(Throwable throwable) {
        errorHandler.onError(throwable);
    }

    protected abstract void inject();
}
