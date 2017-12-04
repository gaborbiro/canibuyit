package com.gb.canibuythat.ui;

import android.os.Bundle;
import android.support.annotation.StringRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.jetbrains.annotations.NotNull;

public class PromptDialog extends BaseDialogFragment {

    static final String EXTRA_TITLE = "title";
    static final String EXTRA_MESSAGE = "message";
    static final String EXTRA_BIG_MESSAGE = "big_message";

    private @StringRes int btnStringResId = android.R.string.ok;
    private View.OnClickListener onClickListener;

    public static PromptDialog messageDialog(String title, String message) {
        PromptDialog promptDialog = new PromptDialog();

        Bundle args = new Bundle();
        args.putString(EXTRA_TITLE, title);
        args.putString(EXTRA_MESSAGE, message);
        promptDialog.setArguments(args);

        return promptDialog;
    }

    public static PromptDialog bigMessageDialog(String title, String message) {
        PromptDialog promptDialog = new PromptDialog();

        Bundle args = new Bundle();
        args.putString(EXTRA_TITLE, title);
        args.putString(EXTRA_BIG_MESSAGE, message);
        promptDialog.setArguments(args);

        return promptDialog;
    }

    @Override
    public @NotNull View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        Bundle args = getArguments();

        setTitle(args.getString(EXTRA_TITLE));
        if (args.containsKey(EXTRA_MESSAGE)) {
            setMessage(args.getString(EXTRA_MESSAGE));
        } // TODO: else here maybe?
        if (args.containsKey(EXTRA_BIG_MESSAGE)) {
            setBigMessage(args.getString(EXTRA_BIG_MESSAGE));
        }

        super.setPositiveButton(btnStringResId, v -> {
            dismiss();
            if (onClickListener != null) {
                onClickListener.onClick(v);
            }
        });
        return view;
    }

    @Override
    public PromptDialog setPositiveButton(int buttonTextId, View.OnClickListener listener) {
        btnStringResId = buttonTextId;
        onClickListener = listener;
        return this;
    }
}
