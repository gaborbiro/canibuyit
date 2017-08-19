package com.gb.canibuythat.di;

import android.app.Application;

public enum Injector {

    INSTANCE;

    private CanIBuyThatGraph graph;

    public void initializeCanIBuyThatComponent(Application application) {
        graph = DaggerCanIBuyThatComponent.builder().application(application).build();
    }

    public CanIBuyThatGraph getGraph() {
        return graph;
    }
}
