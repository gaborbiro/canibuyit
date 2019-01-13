package com.gb.canibuyit.util

fun <A, B> Pair<A?, B?>.doIfBoth(action: (Pair<A, B>) -> Unit) =
    takeIf { first != null && second != null }
            ?.let {
                action(Pair(first!!, second!!))
            }

fun <T> List<T>.compare(other: List<T>, comparator: Comparator<T>): Boolean {
    if (size != other.size) {
        return false
    }
    for (i in 0 until size) {
        if (comparator.compare(get(i), other[i]) != 0) {
            return false
        }
    }
    return true
}