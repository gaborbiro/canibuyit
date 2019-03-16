package com.gb.canibuyit.feature.dispatch.screen

import com.gb.canibuyit.screen.ProgressScreen

interface DispatchScreen : ProgressScreen {
    fun showToast(message: String)
}