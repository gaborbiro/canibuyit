package com.gb.canibuythat.presenter;

import com.gb.canibuythat.exception.ErrorHandler;
import com.gb.canibuythat.screen.Screen;

import javax.inject.Inject;

public abstract class BasePresenter<S extends Screen> {

    @Inject ErrorHandler errorHandler;

    private S screen;

    void onError(Throwable throwable) {
        errorHandler.onError(throwable);
    }

    public void setScreen(S screen) {
        this.screen = screen;
    }

    public S getScreen() {
        return screen;
    }
}