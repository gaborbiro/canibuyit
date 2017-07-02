package com.gb.canibuythat.ui.task;

public abstract class Callback<R> {

    public void onSuccess(R data) {
    }

    public void onFailure() {
    }

    public void onError(Throwable t) {
    }
}
