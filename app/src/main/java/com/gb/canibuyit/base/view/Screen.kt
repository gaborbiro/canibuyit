package com.gb.canibuyit.base.view

import androidx.lifecycle.LifecycleObserver

interface Screen {
    fun addLifecycleObserver(observer: LifecycleObserver)
}
