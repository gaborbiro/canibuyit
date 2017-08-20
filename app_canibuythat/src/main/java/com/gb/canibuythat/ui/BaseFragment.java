package com.gb.canibuythat.ui;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.View;

import com.gb.canibuythat.di.Injector;
import com.gb.canibuythat.exception.ErrorHandler;
import com.gb.canibuythat.exception.FragmentManagerSource;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.Unbinder;

public abstract class BaseFragment extends Fragment implements FragmentManagerSource {

    @Inject ErrorHandler errorHandler;
    private Unbinder unbinder;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        inject();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        unbinder = ButterKnife.bind(this, view);
    }

    @Override
    public void onResume() {
        super.onResume();
        Injector.INSTANCE.registerDialogHandler(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        Injector.INSTANCE.unregisterDialogHandler(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    protected void onError(Throwable throwable) {
        errorHandler.onError(throwable);
    }

    @Override
    public FragmentManager getSupportFragmentManager() {
        return super.getFragmentManager();
    }



    protected abstract void inject();
}
