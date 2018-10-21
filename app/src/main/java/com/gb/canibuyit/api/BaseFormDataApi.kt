package com.gb.canibuyit.api

import okhttp3.MediaType
import okhttp3.RequestBody

abstract class BaseFormDataApi {

    protected fun text(value: String): RequestBody {
        return RequestBody.create(MediaType.parse("text/plain"), value)
    }
}
