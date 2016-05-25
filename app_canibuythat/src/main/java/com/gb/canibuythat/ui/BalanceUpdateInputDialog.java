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
import com.gb.canibuythat.model.BalanceUpdateEvent;
import com.gb.canibuythat.ui.task.balance_update.LastBalanceUpdateLoaderTask;
import com.gb.canibuythat.util.DateUtils;

import java.util.Date;

public class BalanceUpdateInputDialog extends DialogFragment
        implements View.OnClickListener {

    private BalanceUpdateInputListener mListener;
    private BalanceUpdateEvent mLastUpdate;
    private TextView mLastUpdateView;
    private EditText mValueView;
    private DatePickerButton mWhenButton;

    public BalanceUpdateInputDialog() {
        super();
        LastBalanceUpdateLoaderTask mLastBalanceUpdateLoaderTask =
                new LastBalanceUpdateLoaderTask() {
                    @Override
                    protected void onPostExecute(BalanceUpdateEvent balanceUpdateEvent) {
                        mLastUpdate = balanceUpdateEvent;
                        refreshLastUpdate();
                    }
                };
        mLastBalanceUpdateLoaderTask.execute();
    }

    @NonNull @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        LinearLayout body = (LinearLayout) LayoutInflater.from(getActivity())
                .inflate(R.layout.dialog_balance_update, null);
        mLastUpdateView = (TextView) body.findViewById(R.id.last_update);
        mValueView = (EditText) body.findViewById(R.id.value);
        mWhenButton = (DatePickerButton) body.findViewById(R.id.when);
        Date today = new Date();
        mWhenButton.setText(DateUtils.DEFAULT_DATE_FORMAT.format(today));
        mWhenButton.setDate(today);
        refreshLastUpdate();

        return new AlertDialog.Builder(getActivity()).setTitle("Set starting balance")
                .setPositiveButton(getText(android.R.string.ok), null)
                .setNegativeButton(getText(android.R.string.cancel), null)
                .setView(body)
                .create();
    }

    @Override public void onStart() {
        super.onStart();
        final AlertDialog dialog = (AlertDialog) getDialog();

        if (dialog != null) {
            Button b = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            b.setOnClickListener(this);
        }
    }

    @Override public void onAttach(Activity activity) {
        super.onAttach(activity);
        mListener = (BalanceUpdateInputListener) activity;
    }

    @Override public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    private void refreshLastUpdate() {
        if (isAdded()) {
            if (mLastUpdate != null) {
                mLastUpdateView.setText(getString(R.string.reference, mLastUpdate.value,
                        DateUtils.DEFAULT_DATE_FORMAT.format(mLastUpdate.when)));
            } else {
                mLastUpdateView.setText("None");
            }
        }
    }

    @Override public void onClick(View view) {
        if (validate()) {
            BalanceUpdateEvent balanceUpdateEvent = new BalanceUpdateEvent();
            Date selectedDate = mWhenButton.getSelectedDate();
            balanceUpdateEvent.when = selectedDate != null ? selectedDate : null;
            balanceUpdateEvent.value = Float.valueOf(mValueView.getText()
                    .toString());
            if (mListener != null) {
                mListener.onBalanceUpdateSet(balanceUpdateEvent);
            }
            dismiss();
        }
    }

    private boolean validate() {
        if (TextUtils.isEmpty(mValueView.getText())) {
            mValueView.setError("Please specify an amount!");
            mValueView.requestFocus();
            return false;
        }

        Date selectedDate = mWhenButton.getSelectedDate();
        if (!selectedDate.before(new Date())) {
            Toast.makeText(getActivity(), "Please select a date in the past!",
                    Toast.LENGTH_SHORT)
                    .show();
            return false;
        }
        return true;
    }

    public interface BalanceUpdateInputListener {
        void onBalanceUpdateSet(BalanceUpdateEvent event);
    }
}
