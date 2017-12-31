package com.gb.canibuythat.util

fun <T> nonNullAndTrue(data: T?, flag: Boolean? = true): T? {
    return if (data != null && flag == true) data else null
}

fun CharSequence.orNull(): CharSequence? = if (this.isEmpty()) null else this