package com.gb.canibuythat.ui;


import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.View;

import com.gb.canibuythat.di.Injector;
import com.gb.canibuythat.exception.ErrorHandler;
import com.gb.canibuythat.exception.ContextSource;
import com.gb.canibuythat.screen.Screen;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.Unbinder;

public abstract class BaseFragment extends Fragment implements Screen, ContextSource {

    @Inject ErrorHandler errorHandler;

    private ProgressDialog progressDialog;
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
        Injector.INSTANCE.registerContextSource(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        Injector.INSTANCE.unregisterContextSource(this);
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
        return getFragmentManager();
    }

    @Override
    public Context getBaseContext() {
        return getActivity();
    }

    @Override
    public void showProgress() {
        if (progressDialog != null && progressDialog.isAdded()) {
            progressDialog.dismiss();
        }
        progressDialog = ProgressDialog.newInstance("Please wait");
        progressDialog.show(getSupportFragmentManager(), "progress");
    }

    @Override
    public void hideProgress() {
        if (progressDialog != null && progressDialog.isAdded()) {
            progressDialog.dismiss();
        }
    }

    protected abstract void inject();
}
