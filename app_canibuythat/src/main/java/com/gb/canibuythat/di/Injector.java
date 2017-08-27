package com.gb.canibuythat.di;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.support.v4.app.FragmentManager;

import com.gb.canibuythat.exception.ContextSource;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public enum Injector {

    INSTANCE;

    private CanIBuyThatGraph graph;
    private List<ContextSource> contextSources = new ArrayList<>();

    public void initializeCanIBuyThatComponent(Application application) {
        graph = DaggerCanIBuyThatComponent.builder().application(application).build();
    }

    public void registerContextSource(ContextSource contextSource) {
        this.contextSources.add(contextSource);
    }

    public void unregisterContextSource(ContextSource contextSource) {
        this.contextSources.remove(contextSource);
    }

    public CanIBuyThatGraph getGraph() {
        return graph;
    }

    public FragmentManager getFragmentManager() {
        if (contextSources.size() > 0) {
            return contextSources.get(contextSources.size() - 1).getSupportFragmentManager();
        } else {
            return null;
        }
    }

    public Context getContext() {
        if (contextSources.size() > 0) {
            return contextSources.get(contextSources.size() - 1).getBaseContext();
        } else {
            return null;
        }
    }
}
