package com.gb.canibuythat.interactor.model

import com.gb.canibuythat.model.Spending

class LceSpendings : Lce<List<Spending>> {
    constructor(data: List<Spending>) : super(data)
    constructor(error: Throwable) : super(error)
    constructor(loading: Boolean) : super(loading)
}
