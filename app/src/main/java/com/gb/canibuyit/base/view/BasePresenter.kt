package com.gb.canibuyit.base.view

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.gb.canibuyit.base.error.ErrorHandler
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import javax.inject.Inject
import kotlin.reflect.KProperty

abstract class BasePresenter : LifecycleObserver {

    @Inject lateinit var errorHandler: ErrorHandler

    var screenReference: Screen? = null
        set(value) {
            field = value
            value?.addLifecycleObserver(this)
        }

    private val onDestroyDisposables: CompositeDisposable = CompositeDisposable()

    internal fun onError(throwable: Throwable) {
        errorHandler.onError(throwable)
    }

    protected fun disposeOnDestroy(disposable: Disposable) {
        onDestroyDisposables.add(disposable)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        onDestroyDisposables.clear()
        screenReference = null
    }

    protected interface ScreenDelegate<S : Screen> {
        operator fun getValue(presenter: BasePresenter, property: KProperty<*>): S
    }

    protected inline fun <reified S : Screen> screenDelegate(): ScreenDelegate<S> {
        return object : ScreenDelegate<S> {
            private val screen by lazy { screenReference as S }

            override fun getValue(presenter: BasePresenter, property: KProperty<*>) = screen
        }
    }
}