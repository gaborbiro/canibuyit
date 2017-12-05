package com.gb.canibuythat.util

import android.content.Context
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.TextView

fun View.hideKeyboard() {
    this.post {
        val imm = this.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(this.windowToken, 0)
    }
}

fun View.showKeyboard() {
    this.post {
        val imm = this.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
        this.requestFocus()
    }
}

fun TextView.setTextWithLink(text: String, linkPart: String, runOnClick: () -> Unit) {
    this.setTextWithLinks(text, arrayOf(linkPart), arrayOf(runOnClick))
}

fun TextView.setTextWithLinks(text: String, linkParts: Array<String>, runOnClicks: Array<() -> Unit>) {
    this.text = text
    val spannable = SpannableString(this.text)

    for (i in linkParts.indices) {
        applyLink(this.text.toString(), spannable, linkParts[i], runOnClicks[i])
    }

    this.setText(spannable, TextView.BufferType.SPANNABLE)
    this.movementMethod = LinkMovementMethod.getInstance()
}

private fun applyLink(text: String, spannable: Spannable, linkPart: String, runOnClick: () -> Unit) {
    val startIndex = text.indexOf(linkPart)
    if (startIndex < 0) {
        throw IllegalArgumentException("linkPart must be included in text")
    }
    spannable.setSpan(object : ClickableSpan() {
        override fun onClick(widget: View) {
            try {
                runOnClick()
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }
    }, startIndex, startIndex + linkPart.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
}