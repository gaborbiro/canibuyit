package com.gb.canibuythat.ui;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.Toast;

import com.gb.canibuythat.exception.DomainException;
import com.gb.canibuythat.util.Logger;

import butterknife.ButterKnife;
import butterknife.Unbinder;

public abstract class BaseFragment extends Fragment {

    private static final String TAG = "CanIBuyThat";

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
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    protected void showError(Throwable exception) {
        if (exception instanceof DomainException) {
            DomainException domainException = (DomainException) exception;
            switch (domainException.getKind()) {
                case HTTP:
                    Toast.makeText(getActivity(), "HTTP error (" + domainException.getCode() + "): " + domainException.getMessage(), Toast.LENGTH_LONG).show();
                    break;
                case NETWORK:
                    Toast.makeText(getActivity(), "NETWORK error" + exception.getMessage(), Toast.LENGTH_LONG).show();
                    break;
                case GENERIC:
                    Toast.makeText(getActivity(), "GENERIC error" + exception.getMessage(), Toast.LENGTH_LONG).show();
                    break;
            }
        }
        if (exception.getCause() != null) {
            Logger.e(TAG, exception.getMessage(), exception.getCause());
        }
    }

    protected abstract void inject();
}
