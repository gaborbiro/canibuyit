package com.gb.canibuyit.util

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