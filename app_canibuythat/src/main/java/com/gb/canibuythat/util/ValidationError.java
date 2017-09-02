package com.gb.canibuythat.util;

import android.content.Context;
import android.support.annotation.IntDef;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * There are two kinds of validation errors: the ones that can be shown in an input
 * field (with {@link TextView#setError(CharSequence)} and the ones tha are shown
 * as a Toast or Dialog instead (DatePicker, etc...).
 */
public class ValidationError {
    public static final int TYPE_INPUT_FIELD = 1;
    public static final int TYPE_NON_INPUT_FIELD = 2;

    @IntDef({TYPE_INPUT_FIELD, TYPE_NON_INPUT_FIELD})
    @Retention(RetentionPolicy.SOURCE)
    private @interface ValidationErrorType {
    }

    final int type;
    final View target;
    final String errorMessage;

    public ValidationError(@ValidationErrorType int type, View target, String errorMessage) {
        if (type == TYPE_INPUT_FIELD) {
            if (target == null) {
                throw new IllegalArgumentException("Please specify a target view for the input field error " + "message");
            }
            if (!(target instanceof TextView)) {
                throw new IllegalArgumentException("Wrong view type in ValidationError. Cannot show error " + "message.");

            }
        }
        this.type = type;
        this.target = target;
        this.errorMessage = errorMessage;
    }

    public void showError(Context context) throws IllegalArgumentException {
        if (type == TYPE_INPUT_FIELD) {
            TextView textView = (TextView) target;
            textView.setError(errorMessage);
            textView.requestFocus();
        } else if (type == TYPE_NON_INPUT_FIELD) {
            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show();
            if (target != null) {
                target.requestFocus();
            }
        }
    }
}