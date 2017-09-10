package com.gb.canibuythat.interactor

open class Lce<T> {

    var loading: Boolean = false
    var content: T? = null
    var error: Throwable? = null

    private constructor()

    private constructor(loading: Boolean) {
        this.loading = loading
    }

    private constructor(data: T) {
        this.content = data
    }

    private constructor(error: Throwable) {
        this.error = error
    }

    internal fun hasError(): Boolean {
        return error != null
    }

    companion object {
        fun <T> loading(): Lce<T> {
            return Lce<T>(true)
        }

        fun <T> content(content: T): Lce<T> {
            return Lce(content)
        }

        fun <T> nothing(): Lce<T> {
            return Lce()
        }

        fun <T> error(error: Throwable): Lce<T> {
            return Lce<T>(error)
        }
    }
}
