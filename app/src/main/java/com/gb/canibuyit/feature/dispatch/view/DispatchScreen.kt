package com.gb.canibuyit.feature.dispatch.view

import com.gb.canibuyit.base.view.ProgressScreen

interface DispatchScreen : ProgressScreen {
    fun showToast(message: String)
}