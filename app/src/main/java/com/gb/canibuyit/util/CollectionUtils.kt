package com.gb.canibuyit.util

fun <A, B> Pair<A?, B?>.doIfBoth(action: (Pair<A, B>) -> Unit) =
    takeIf { first != null && second != null }
            ?.let {
                action(Pair(first!!, second!!))
            }