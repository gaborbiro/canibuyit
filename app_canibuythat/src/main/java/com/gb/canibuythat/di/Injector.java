package com.gb.canibuythat.di;

import android.app.Application;
import android.support.v4.app.FragmentManager;

import com.gb.canibuythat.exception.FragmentManagerSource;

import java.util.ArrayList;
import java.util.List;

public enum Injector {

    INSTANCE;

    private CanIBuyThatGraph graph;
    private List<FragmentManagerSource> fragmentManagerSources = new ArrayList<>();

    public void initializeCanIBuyThatComponent(Application application) {
        graph = DaggerCanIBuyThatComponent.builder().application(application).build();
    }

    public void registerDialogHandler(FragmentManagerSource fragmentManagerSource) {
        this.fragmentManagerSources.add(fragmentManagerSource);
    }

    public void unregisterDialogHandler(FragmentManagerSource fragmentManagerSource) {
        this.fragmentManagerSources.remove(fragmentManagerSource);
    }

    public CanIBuyThatGraph getGraph() {
        return graph;
    }

    public FragmentManager getFragmentManager() {
        if (fragmentManagerSources.size() > 0) {
            return fragmentManagerSources.get(fragmentManagerSources.size() - 1).getSupportFragmentManager();
        } else {
            return null;
        }
    }
}
