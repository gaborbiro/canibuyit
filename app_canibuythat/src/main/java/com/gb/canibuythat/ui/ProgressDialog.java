package com.gb.canibuythat.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class ProgressDialog extends BaseDialogFragment {

    private static final String EXTRA_TITLE = "title";

    public static ProgressDialog newInstance(String title) {
        ProgressDialog progressDialog = new ProgressDialog();
        Bundle args = new Bundle();
        args.putString(EXTRA_TITLE, title);
        progressDialog.setArguments(args);
        return progressDialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        Bundle b = getArguments();
        setHeading(b.getString(EXTRA_TITLE));
        body.setVisibility(View.GONE);
        positiveBtn.setVisibility(View.GONE);
        activityIndicator.setVisibility(View.VISIBLE);
        return view;
    }
}
