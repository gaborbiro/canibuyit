package com.gb.canibuyit.util

import java.math.BigDecimal

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

inline fun <T> Iterable<T>.sumBy(selector: (T) -> BigDecimal): BigDecimal {
    var sum = BigDecimal.ZERO
    for (element in this) {
        sum += selector(element)
    }
    return sum
}

inline fun <T> Array<T>.sumBy(selector: (T) -> BigDecimal): BigDecimal {
    var sum = BigDecimal.ZERO
    for (element in this) {
        sum += selector(element)
    }
    return sum
}
