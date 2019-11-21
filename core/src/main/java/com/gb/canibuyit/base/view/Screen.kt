package com.gb.canibuyit.base.view

import androidx.lifecycle.LifecycleObserver

interface Screen {
    fun showProgress()
    fun hideProgress()
    fun addLifecycleObserver(observer: LifecycleObserver)
}
