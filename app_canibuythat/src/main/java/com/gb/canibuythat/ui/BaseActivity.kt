package com.gb.canibuythat.ui

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import butterknife.ButterKnife
import butterknife.Unbinder
import com.gb.canibuythat.di.Injector
import com.gb.canibuythat.exception.ContextSource
import com.gb.canibuythat.exception.ErrorHandler
import com.gb.canibuythat.presenter.BasePresenter
import com.gb.canibuythat.screen.ProgressScreen
import com.gb.canibuythat.screen.Screen
import org.jetbrains.annotations.Nullable
import javax.inject.Inject

abstract class BaseActivity : AppCompatActivity(), ProgressScreen, ContextSource {

    lateinit @Inject var errorHandler: ErrorHandler

    private var progressDialog: ProgressDialog? = null
    lateinit var unbinder: Unbinder
    var presenter: BasePresenter<Screen>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        presenter = inject()
        presenter?.let { presenter!!.screen = this }
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
        try {
            presenter!!.onPresenterDestroyed()
        } catch (t: Throwable) {
            // ignore
        }
    }

    protected fun onError(throwable: Throwable) {
        errorHandler.onError(throwable)
    }

    override fun showProgress() {
        if (progressDialog == null) {
            progressDialog = ProgressDialog.newInstance("Please wait")
            progressDialog!!.show(supportFragmentManager, "progress")
        }
    }

    override fun hideProgress() {
        if (progressDialog != null && progressDialog!!.isAdded && !progressDialog!!.isRemoving) {
            progressDialog!!.dismissAllowingStateLoss()
            progressDialog = null
        }
    }

    @Nullable protected abstract fun inject(): BasePresenter<Screen>
}
