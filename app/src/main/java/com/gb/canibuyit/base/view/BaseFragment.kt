package com.gb.canibuyit.base.view

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleObserver
import com.gb.canibuyit.di.Injector
import com.gb.canibuyit.error.ContextSource

abstract class BaseFragment : Fragment(), ContextSource, Screen {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inject()
    }

    override fun onResume() {
        super.onResume()
        Injector.INSTANCE.registerContextSource(this)
    }

    override fun onPause() {
        super.onPause()
        Injector.INSTANCE.unregisterContextSource(this)
    }

    override fun getSupportFragmentManager(): FragmentManager? {
        return activity?.supportFragmentManager
    }

    override fun getBaseContext(): Context? {
        return activity
    }

    override fun addLifecycleObserver(observer: LifecycleObserver) {
        lifecycle.addObserver(observer)
    }

    protected abstract fun inject()
}
