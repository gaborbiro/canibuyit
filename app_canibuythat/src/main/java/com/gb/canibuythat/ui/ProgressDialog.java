package com.gb.canibuythat.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class ProgressDialog extends BaseDialogFragment {

    private static final String EXTRA_MESSAGE = "message";

    public static ProgressDialog newInstance(String message) {
        ProgressDialog progressDialog = new ProgressDialog();
        Bundle args = new Bundle();
        args.putString(EXTRA_MESSAGE, message);
        progressDialog.setArguments(args);
        return progressDialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        Bundle b = getArguments();
        setMessage(b.getString(EXTRA_MESSAGE));
        positiveBtn.setVisibility(View.GONE);
        activityIndicator.setVisibility(View.VISIBLE);
        return view;
    }
}
