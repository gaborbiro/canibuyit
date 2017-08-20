package com.gb.canibuythat.presenter;

import com.gb.canibuythat.exception.ErrorHandler;

import javax.inject.Inject;

public abstract class BasePresenter {

    @Inject ErrorHandler errorHandler;

    public BasePresenter() {
        inject();
    }

    protected void onError(Throwable throwable) {
        errorHandler.onError(throwable);
    }

    protected abstract void inject();
}
