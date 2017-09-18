package com.gb.canibuythat.ui;

import android.os.Bundle;
import android.support.annotation.StringRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class InputDialog extends BaseDialogFragment {

    private static final String EXTRA_TITLE = "title";
    private static final String EXTRA_CURRENT_INPUT = "current_input";

    private @StringRes int btnStringResId;
    private View.OnClickListener onClickListener;

    public static InputDialog newInstance(String title, String currentInput) {
        InputDialog inputDialog = new InputDialog();

        Bundle args = new Bundle();
        args.putString(EXTRA_TITLE, title);
        args.putString(EXTRA_CURRENT_INPUT, currentInput);
        inputDialog.setArguments(args);

        return inputDialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        Bundle b = getArguments();

        setTitle(b.getString(EXTRA_TITLE));
        input.setText(b.getString(EXTRA_CURRENT_INPUT));
        input.setVisibility(View.VISIBLE);

        super.setPositiveButton(btnStringResId, v -> {
            dismiss();
            if (onClickListener != null) {
                onClickListener.onClick(v);
            }
        });
        return view;
    }

    @Override
    public InputDialog setPositiveButton(int buttonTextId, View.OnClickListener listener) {
        btnStringResId = buttonTextId;
        onClickListener = listener;
        return this;
    }

    public String getInput() {
        return input.getText().toString();
    }
}
