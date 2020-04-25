package com.gb.canibuyit.util

import android.app.DatePickerDialog
import android.content.Context
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.DatePicker
import android.widget.TextView
import androidx.annotation.LayoutRes
import java.time.LocalDate

fun View.hideKeyboard() {
    this.post {
        val imm = this.context.getSystemService(InputMethodManager::class.java)
        imm.hideSoftInputFromWindow(this.windowToken, 0)
    }
}

fun View.showKeyboard() {
    this.post {
        val imm = this.context.getSystemService(InputMethodManager::class.java)
        imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
        this.requestFocus()
    }
}

fun String.link() = this.span().link()

fun SpannableString.link() = this.apply {
    setSpan(UnderlineSpan(), 0, length, 0)
}

fun String.bold(vararg subString: String) = this.span().bold(*subString)

fun SpannableString.bold(vararg subString: String) = this.apply {
    subString.forEach {
        val start = this.indexOf(it)
        val end = start + it.length
        setSpan(StyleSpan(android.graphics.Typeface.BOLD), start, end,
            Spannable.SPAN_INCLUSIVE_INCLUSIVE)
    }
}

fun TextView.setSubtextWithLink(text: String, linkPart: String, runOnClick: () -> Unit) {
    this.setSubtextWithLinks(text, arrayOf(linkPart), arrayOf(runOnClick))
}

fun TextView.setSubtextWithLinks(text: String, linkParts: Array<String>,
                                 runOnClicks: Array<() -> Unit>) {
    this.text = text
    val spannable = this.text.span()

    for (i in linkParts.indices) {
        applyLink(this.text.toString(), spannable, linkParts[i], runOnClicks[i])
    }

    this.setText(spannable, TextView.BufferType.SPANNABLE)
    this.movementMethod = LinkMovementMethod.getInstance()
}

private fun applyLink(text: String, spannable: Spannable, linkPart: String,
                      runOnClick: () -> Unit) {
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

private fun String.span() = SpannableString(this)

private fun CharSequence.span() = SpannableString(this)

fun createDatePickerDialog(context: Context, date: LocalDate,
                           listener: (DatePicker, LocalDate) -> Unit): DatePickerDialog {
    return DatePickerDialog(context, { view, year, month, dayOfMonth ->
        listener.invoke(view, LocalDate.of(year, month + 1, dayOfMonth))
    }, date.year, date.monthValue - 1, date.dayOfMonth)
}

inline fun <reified T : View> ViewGroup.inflate(@LayoutRes resource: Int,
                                                attachToRoot: Boolean = false): T =
    LayoutInflater.from(this.context).inflate(resource, this, attachToRoot) as T

@Suppress("UNCHECKED_CAST")
inline fun <reified T : View> ViewGroup.add(@LayoutRes resource: Int): T =
    (inflate(resource) as T).also { addView(it) }