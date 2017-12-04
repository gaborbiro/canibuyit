package com.gb.canibuythat.ui;

import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.gb.canibuythat.R;

import org.jetbrains.annotations.NotNull;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public abstract class BaseDialogFragment extends DialogFragment {

    private Unbinder unbinder;

    @BindView(R.id.button) Button positiveBtn;
    @BindView(R.id.progress_bar) ProgressBar activityIndicator;
    @BindView(R.id.title) TextView title;
    @BindView(R.id.message) TextView message;
    @BindView(R.id.big_message) TextView bigMessage;
    @BindView(R.id.big_message_container) View bigMessageContainer;
    @BindView(R.id.input) EditText input;

    @Override
    public @NotNull View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.prompt_dialog_layout, container, false);
        unbinder = ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    public BaseDialogFragment setTitle(@StringRes int title) {
        setTitle(getString(title));
        return this;
    }

    public BaseDialogFragment setTitle(String title) {
        this.title.setText(title);
        this.title.setVisibility(View.VISIBLE);
        return this;
    }

    public BaseDialogFragment setMessage(@StringRes int message) {
        setMessage(getString(message));
        return this;
    }

    public BaseDialogFragment setBigMessage(@StringRes int message) {
        setBigMessage(getString(message));
        return this;
    }

    public BaseDialogFragment setMessage(String body) {
        this.message.setText(body);
        this.message.setVisibility(View.VISIBLE);
        return this;
    }

    public BaseDialogFragment setBigMessage(String body) {
        this.bigMessage.setText(body);
        this.bigMessage.setVisibility(View.VISIBLE);
        this.bigMessageContainer.setVisibility(View.VISIBLE);
        return this;
    }

    public BaseDialogFragment setPositiveButton(int buttonTextId, View.OnClickListener listener) {
        if (listener != null) {
            positiveBtn.setOnClickListener(listener);
        }
        positiveBtn.setText(getString(buttonTextId));
        positiveBtn.setVisibility(View.VISIBLE);
        activityIndicator.setVisibility(View.GONE);
        return this;
    }

    @Override
    public void show(FragmentManager manager, String tag) {
        if (!isAdded()) {
            super.show(manager, tag);
        }
    }
}
