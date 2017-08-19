package com.gb.canibuythat.util;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

public class ViewUtils {

    public static void showKeyboard(final View view) {
        view.post(() -> {
            InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
            view.requestFocus();
        });
    }

    public static void hideKeyboard(final View view) {
        view.post(() -> {
            InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        });
    }

    public static void setTextWithLink(TextView textView, String text, String linkPart, final Runnable runOnClick) {
        textView.setText(text);
        Spannable spannable = new SpannableString(textView.getText());
        int startIndex = textView.getText().toString().indexOf(linkPart);
        if (startIndex < 0) {
            throw new IllegalArgumentException("linkPart must be included in text");
        }
        spannable.setSpan(new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                try {
                    runOnClick.run();
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }, startIndex, startIndex + linkPart.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        textView.setText(spannable, TextView.BufferType.SPANNABLE);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
    }
}
