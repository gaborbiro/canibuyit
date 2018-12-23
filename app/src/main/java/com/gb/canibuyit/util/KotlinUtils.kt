package com.gb.canibuyit.util

import android.view.View
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

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

fun View.visible() = this.visibility != View.GONE

inline fun <reified T> Gson.fromJson(json: String): T =
    this.fromJson<T>(json, object : TypeToken<T>() {}.type)

fun <A, B> Pair<A?, B?>.eitherOrNull(action: (Pair<A, B>) -> Unit) =
    takeIf { first != null || second != null }
            ?.let { action(Pair(first!!, second!!)) }