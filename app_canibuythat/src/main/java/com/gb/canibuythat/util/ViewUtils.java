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

    public static void hideKeyboard(final View view) {
        view.post(() -> {
            InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        });
    }


    public static void setTextWithLink(TextView textView, String text, String linkPart, Runnable runOnClick) {
        setTextWithLinks(textView, text, new String[]{linkPart}, new Runnable[]{runOnClick});
    }

    public static void setTextWithLinks(TextView textView, String text, String[] linkParts, Runnable[] runOnClicks) {
        textView.setText(text);
        Spannable spannable = new SpannableString(textView.getText());

        for (int i = 0; i < linkParts.length; i++) {
            applyLink(textView.getText().toString(), spannable, linkParts[i], runOnClicks[i]);
        }

        textView.setText(spannable, TextView.BufferType.SPANNABLE);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private static void applyLink(String text, Spannable spannable, String linkPart, Runnable runOnClick) {
        int startIndex = text.indexOf(linkPart);
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
    }
}
