package com.gb.canibuyit.base.view

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import butterknife.ButterKnife
import butterknife.Unbinder
import com.gb.canibuyit.base.ui.ProgressDialog
import com.gb.canibuyit.di.Injector
import com.gb.canibuyit.error.ContextSource
import com.gb.canibuyit.error.ErrorHandler
import javax.inject.Inject

abstract class BaseActivity : AppCompatActivity(), ProgressScreen, ContextSource {

    @Inject lateinit var errorHandler: ErrorHandler

    private var progressDialog: ProgressDialog? = null
    private lateinit var unbinder: Unbinder
    var presenter: BasePresenter<Screen>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        presenter = inject()
        presenter?.setScreen(this)
    }

    override fun onResume() {
        super.onResume()
        Injector.INSTANCE.registerContextSource(this)
    }

    override fun onPause() {
        super.onPause()
        Injector.INSTANCE.unregisterContextSource(this)
    }

    override fun onContentChanged() {
        super.onContentChanged()
        unbinder = ButterKnife.bind(this)
    }

    override fun onDestroy() {
        unbinder.unbind()
        super.onDestroy()
        presenter?.onPresenterDestroyed()
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

    protected abstract fun inject(): BasePresenter<Screen>?
}
