package com.gb.canibuyit.model

import io.reactivex.Observable
import io.reactivex.Single

open class Lce<T> {

    var loading: Boolean = false
        private set

    var content: T? = null
        private set

    var error: Throwable? = null
        private set

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

        fun <T> error(error: Throwable): Lce<T> {
            return Lce<T>(error)
        }
    }
}

fun <T> Observable<Lce<T>>.onNextContent(): Single<T> {
    return filter { !it.loading && !it.hasError() }
            .map { it.content!! }
            .firstOrError()
}