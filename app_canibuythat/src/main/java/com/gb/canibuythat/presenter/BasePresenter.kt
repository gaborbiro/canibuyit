package com.gb.canibuythat.presenter

import com.gb.canibuythat.exception.ErrorHandler
import com.gb.canibuythat.screen.Screen

import javax.inject.Inject

import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable

abstract class BasePresenter<S : Screen> {

    lateinit @Inject var errorHandler: ErrorHandler

    private val currentDisposables: CompositeDisposable = CompositeDisposable()
    lateinit var screen: S

    internal fun onError(throwable: Throwable) {
        errorHandler.onError(throwable)
    }

    protected fun disposeOnFinish(disposable: Disposable) {
        currentDisposables.add(disposable)
    }

    fun onPresenterDestroyed() {
        currentDisposables.dispose()
    }
}