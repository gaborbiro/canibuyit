package com.gb.canibuythat.presenter

import com.gb.canibuythat.exception.ErrorHandler
import com.gb.canibuythat.screen.Screen
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import javax.inject.Inject

abstract class BasePresenter<S : Screen> {

    lateinit @Inject var errorHandler: ErrorHandler

    private val currentDisposables: CompositeDisposable = CompositeDisposable()
    private var screen: S? = null

    fun setScreen(screen: S) {
        if (this.screen != screen) {
            this.screen = screen
            onScreenSet()
        }
    }

    fun getScreen(): S {
        return screen!!
    }

    internal fun onError(throwable: Throwable) {
        errorHandler.onError(throwable)
    }

    protected fun disposeOnFinish(disposable: Disposable) {
        currentDisposables.add(disposable)
    }

    fun onPresenterDestroyed() {
        currentDisposables.clear()
    }

    open fun onScreenSet() {
        // override in child class if needed
    }
}