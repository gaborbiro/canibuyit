package com.gb.canibuythat.util

import android.util.Log

object Logger {

    private val TAG = "CanIBuyThat"

    fun d(tag: String, message: String) {
        Log.d(TAG, "($tag) $message")
    }

    fun d(tag: String, t: Throwable) {
        Log.d(TAG, "($tag)", t)
    }

    fun e(tag: String, message: String) {
        Log.e(TAG, "($tag) $message")
    }

    fun e(tag: String, message: String, cause: Throwable) {
        Log.e(TAG, "($tag) $message", cause)
    }

    fun e(tag: String, t: Throwable) {
        Log.e(TAG, "($tag)", t)
    }

    fun v(tag: String, message: String) {
        Log.v(TAG, "($tag) $message")
    }
}
