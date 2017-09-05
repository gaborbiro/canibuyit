package com.gb.canibuythat.interactor.model

import com.gb.canibuythat.model.Login

class LceLogin : Lce<Login> {
    constructor(data: Login) : super(data)
    constructor(error: Throwable) : super(error)
    constructor(loading: Boolean) : super(loading)
}