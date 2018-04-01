package com.gb.canibuythat.ui


import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.view.View
import butterknife.ButterKnife
import butterknife.Unbinder
import com.gb.canibuythat.di.Injector
import com.gb.canibuythat.exception.ContextSource
import com.gb.canibuythat.exception.ErrorHandler
import com.gb.canibuythat.presenter.BasePresenter
import com.gb.canibuythat.screen.Screen
import org.jetbrains.annotations.Nullable
import javax.inject.Inject

abstract class BaseFragment : Fragment(), ContextSource, Screen {

    lateinit @Inject internal var errorHandler: ErrorHandler

    private lateinit var unbinder: Unbinder
    var presenter: BasePresenter<Screen>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        presenter = inject()
        presenter?.setScreen(this)
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
        presenter?.onPresenterDestroyed()
    }

    protected fun onError(throwable: Throwable) {
        errorHandler.onError(throwable)
    }

    override fun getSupportFragmentManager(): FragmentManager? {
        return activity?.supportFragmentManager
    }

    override fun getBaseContext(): Context? {
        return activity
    }

    @Nullable protected abstract fun inject(): BasePresenter<Screen>?
}
