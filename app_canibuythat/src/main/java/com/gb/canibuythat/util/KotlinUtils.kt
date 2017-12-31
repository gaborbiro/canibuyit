package com.gb.canibuythat.util

import android.view.View

fun <T> nonNullAndTrue(data: T?, flag: Boolean? = true): T? {
    return if (data != null && flag == true) data else null
}

fun CharSequence.orNull(): CharSequence? = if (this.isEmpty()) null else this

fun <A, B> Pair<A?, B?>.let(action: (pair: Pair<A?, B?>) -> Unit) {
    if (this.first != null && this.second != null)
        action(this)
}

fun View.show() {
    this.visibility = View.VISIBLE
}

fun View.hide() {
    this.visibility = View.GONE
}