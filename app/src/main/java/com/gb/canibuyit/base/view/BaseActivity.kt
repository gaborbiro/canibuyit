package com.gb.canibuyit.base.view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleObserver
import com.gb.canibuyit.base.ui.ProgressDialog
import com.gb.canibuyit.di.Injector
import com.gb.canibuyit.error.ContextSource
import com.gb.canibuyit.error.ErrorHandler
import org.jetbrains.annotations.Nullable
import javax.inject.Inject

abstract class BaseActivity : AppCompatActivity(), ProgressScreen, ContextSource {

    @Inject lateinit var errorHandler: ErrorHandler

    private var progressDialog: ProgressDialog? = null

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

    protected fun onError(throwable: Throwable) {
        errorHandler.onError(throwable)
    }

    override fun showProgress() {
        progressDialog ?: let {
            progressDialog = ProgressDialog.newInstance("Please wait")
                    .apply {
                        show(supportFragmentManager, "progress")
                    }
        }
    }

    override fun hideProgress() {
        progressDialog?.run {
            if (isAdded && !isRemoving) {
                dismissAllowingStateLoss()
            }
        }
        progressDialog = null
    }

    override fun addLifecycleObserver(observer: LifecycleObserver) {
        lifecycle.addObserver(observer)
    }

    protected abstract fun inject()
}
