package com.gb.canibuythat.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.gb.canibuythat.R;
import com.gb.canibuythat.UserPreferences;
import com.gb.canibuythat.ui.model.BalanceReading;
import com.gb.canibuythat.util.DateUtils;

import java.util.Date;

public class BalanceReadingInputDialog extends DialogFragment
        implements View.OnClickListener {

    private BalanceReadingInputListener listener;
    private BalanceReading lastUpdate;
    private TextView lastUpdateView;
    private EditText valueView;
    private DatePickerButton whenButton;

    public BalanceReadingInputDialog() {
        lastUpdate = UserPreferences.getBalanceReading();
        refreshLastUpdate();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LinearLayout body = (LinearLayout) LayoutInflater.from(getActivity()).inflate(R.layout.dialog_balance_reading, null);
        lastUpdateView = (TextView) body.findViewById(R.id.last_update);
        valueView = (EditText) body.findViewById(R.id.value);
        whenButton = (DatePickerButton) body.findViewById(R.id.when);
        Date today = new Date();
        whenButton.setText(DateUtils.FORMAT_MONTH_DAY.format(today));
        whenButton.setDate(today);
        refreshLastUpdate();

        return new AlertDialog.Builder(getActivity()).setTitle("Set starting balance")
                .setPositiveButton(getText(android.R.string.ok), null)
                .setNegativeButton(getText(android.R.string.cancel), null)
                .setView(body)
                .create();
    }

    @Override
    public void onStart() {
        super.onStart();
        final AlertDialog dialog = (AlertDialog) getDialog();

        if (dialog != null) {
            Button b = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            b.setOnClickListener(this);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        listener = (BalanceReadingInputListener) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    private void refreshLastUpdate() {
        if (isAdded()) {
            if (lastUpdate != null) {
                lastUpdateView.setText(getString(R.string.balance_update_reading, lastUpdate.balance,
                        DateUtils.FORMAT_MONTH_DAY.format(lastUpdate.when)));
            } else {
                lastUpdateView.setText("None");
            }
        }
    }

    @Override
    public void onClick(View view) {
        if (validate()) {
            if (listener != null) {
                Date selectedDate = whenButton.getSelectedDate();
                BalanceReading balanceUpdateEvent = new BalanceReading(selectedDate != null ? selectedDate : null,
                        Float.valueOf(valueView.getText().toString()));
                listener.onBalanceReadingSet(balanceUpdateEvent);
            }
            dismiss();
        }
    }

    private boolean validate() {
        if (TextUtils.isEmpty(valueView.getText())) {
            valueView.setError("Please specify an amount!");
            valueView.requestFocus();
            return false;
        }

        Date selectedDate = whenButton.getSelectedDate();
        if (!selectedDate.before(new Date())) {
            Toast.makeText(getActivity(), "Please select a non-future date!", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    interface BalanceReadingInputListener {
        void onBalanceReadingSet(BalanceReading event);
    }
}
