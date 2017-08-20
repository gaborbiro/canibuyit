package com.gb.canibuythat.ui;

import android.os.Bundle;
import android.support.annotation.StringRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class PromptDialog extends BaseDialogFragment {

    private static final String EXTRA_TITLE = "title";
    private static final String EXTRA_MESSAGE = "message";

    private @StringRes int btnStringResId = android.R.string.ok;
    private View.OnClickListener onClickListener;

    public static PromptDialog newInstance(String title, String message) {
        PromptDialog promptDialog = new PromptDialog();

        Bundle args = new Bundle();
        args.putString(EXTRA_TITLE, title);
        args.putString(EXTRA_MESSAGE, message);
        promptDialog.setArguments(args);

        return promptDialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        Bundle b = getArguments();

        setHeading(b.getString(EXTRA_TITLE));
        setMessage(b.getString(EXTRA_MESSAGE));

        super.setPositiveButton(btnStringResId, v -> {
            dismiss();
            if (onClickListener != null) {
                onClickListener.onClick(v);
            }
        });
        return view;
    }

    @Override
    public void setPositiveButton(int buttonTextId, View.OnClickListener listener) {
        btnStringResId = buttonTextId;
        onClickListener = listener;
    }
}
