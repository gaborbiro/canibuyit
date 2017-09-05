package com.gb.canibuythat.interactor.model

open class Lce<T> {

    var data: T? = null
    var error: Throwable? = null
    var isLoading: Boolean = false

    constructor(data: T) {
        this.data = data
    }

    constructor(error: Throwable) {
        this.error = error
    }

    constructor(loading: Boolean) {
        this.isLoading = loading
    }

    internal fun hasError(): Boolean {
        return error != null
    }

    companion object {
        fun <T> data(data: T): Lce<T> {
            return Lce(data)
        }

        fun <T> error(error: Throwable): Lce<T> {
            return Lce<T>(error)
        }

        fun <T> loading(loading: Boolean): Lce<T> {
            return Lce<T>(loading)
        }
    }
}
