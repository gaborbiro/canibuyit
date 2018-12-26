package com.gb.canibuyit.ui

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.view.View
import butterknife.ButterKnife
import butterknife.Unbinder
import com.gb.canibuyit.di.Injector
import com.gb.canibuyit.exception.ContextSource
import com.gb.canibuyit.presenter.BasePresenter
import com.gb.canibuyit.screen.Screen
import org.jetbrains.annotations.Nullable
import javax.inject.Inject

abstract class BaseFragment<S : Screen, P : BasePresenter<S>> : Fragment(), ContextSource, Screen {

    private lateinit var unbinder: Unbinder
    @Inject protected lateinit var presenter: P

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inject()
        presenter.setScreen(this as S)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        unbinder = ButterKnife.bind(this, view)
    }

    override fun onResume() {
        super.onResume()
        Injector.INSTANCE.registerContextSource(this)
    }

    override fun onPause() {
        super.onPause()
        Injector.INSTANCE.unregisterContextSource(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        unbinder.unbind()
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.onPresenterDestroyed()
    }

    override fun getSupportFragmentManager(): FragmentManager? {
        return activity?.supportFragmentManager
    }

    override fun getBaseContext(): Context? {
        return activity
    }

    @Nullable
    protected abstract fun inject()
}
