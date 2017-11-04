package com.gb.canibuythat.util

import android.text.TextUtils

fun String.isEmpty(): Boolean {
    return TextUtils.isEmpty(this)
}