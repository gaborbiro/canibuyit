package com.gb.canibuyit.util

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

fun CharSequence.orNull(): CharSequence? = if (this.isBlank()) null else this

fun <A, B> Pair<A?, B?>.let(action: (pair: Pair<A?, B?>) -> Unit) {
    if (this.first != null && this.second != null)
        action(this)
}

inline fun <reified T> Gson.fromJson(json: String): T =
    this.fromJson<T>(json, object : TypeToken<T>() {}.type)

fun <A, B> Pair<A?, B?>.eitherOrNull(action: (Pair<A, B>) -> Unit) =
    takeIf { first != null || second != null }
            ?.let { action(Pair(first!!, second!!)) }