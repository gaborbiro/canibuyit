package com.gb.canibuythat.ui;

import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.gb.canibuythat.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public abstract class BaseDialogFragment extends DialogFragment {

    private Unbinder unbinder;

    @BindView(R.id.positive_button) Button positiveBtn;
    @BindView(R.id.progress_bar) ProgressBar activityIndicator;
    @BindView(R.id.heading) TextView heading;
    @BindView(R.id.message) TextView body;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.prompt_dialog_layout, container, false);
        unbinder = ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    public void setHeading(@StringRes int headingTextId) {
        setHeading(getString(headingTextId));
    }

    public void setHeading(String headingText) {
        heading.setText(headingText);
        heading.setVisibility(View.VISIBLE);
    }

    public void setMessage(@StringRes int messageTextId) {
        setMessage(getString(messageTextId));
    }

    public void setMessage(String messageText) {
        body.setText(messageText);
        body.setVisibility(View.VISIBLE);
    }

    public void setPositiveButton(int buttonTextId, View.OnClickListener listener) {
        if (listener != null) {
            positiveBtn.setOnClickListener(listener);
        }
        positiveBtn.setText(getString(buttonTextId));
        positiveBtn.setVisibility(View.VISIBLE);
        activityIndicator.setVisibility(View.GONE);
    }

    @Override
    public void show(FragmentManager manager, String tag) {
        if (!isAdded()) {
            super.show(manager, tag);
        }
    }
}
