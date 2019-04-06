package com.gb.canibuyit.base.view

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import butterknife.ButterKnife
import butterknife.Unbinder
import com.gb.canibuyit.di.Injector
import com.gb.canibuyit.error.ContextSource
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
