package com.gb.canibuyit.error

import okhttp3.ResponseBody
import retrofit2.HttpException
import java.io.IOException

open class DomainException @JvmOverloads constructor(
    message: String?,
    cause: Throwable? = null) : Throwable(message, cause) {

    var responseBody: ResponseBody? = null
        private set

    var code: Int
        private set

    var kind: Kind
        private set

    var action: Action? = null
        private set

    constructor(raw: Throwable) : this(raw.message, raw)

    init {
        this.code = -1
        this.kind = Kind.GENERIC

        if (cause is HttpException) {
            this.kind = Kind.HTTP
            this.code = cause.code()

            if (code == 400) {
                action = Action.LOGIN
            }
            this.responseBody = cause.response()?.errorBody()
        }
        if (cause is IOException) {
            this.kind = Kind.NETWORK
        }
    }

    enum class Kind {
        NETWORK,
        HTTP,
        GENERIC
    }

    enum class Action {
        LOGIN
    }
}
