package com.gb.canibuyit.feature.monzo.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import com.gb.canibuyit.R
import com.gb.canibuyit.di.Injector
import com.gb.canibuyit.feature.monzo.CLIENT_ID
import com.gb.canibuyit.feature.monzo.CredentialsProvider
import com.gb.canibuyit.feature.monzo.MONZO_OAUTH_URL
import com.gb.canibuyit.feature.monzo.MONZO_URI_AUTH_CALLBACK
import com.gb.canibuyit.presenter.BasePresenter
import com.gb.canibuyit.screen.Screen
import com.gb.canibuyit.ui.BaseActivity
import com.gb.canibuyit.util.Logger
import kotlinx.android.synthetic.main.activity_web.*
import java.net.URLEncoder
import javax.inject.Inject

class LoginActivity : BaseActivity() {

    @Inject lateinit var credentialsProvider: CredentialsProvider

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web)

        credentialsProvider.accessToken = null

        webview.webChromeClient = WebChromeClient()
        webview.scrollBarStyle = WebView.SCROLLBARS_OUTSIDE_OVERLAY
        webview.isScrollbarFadingEnabled = false
        webview.webViewClient = WebViewClient()
        val webSettings = webview.settings
        webSettings.javaScriptEnabled = true

        val url = (MONZO_OAUTH_URL
                + "/?client_id=" + CLIENT_ID
                + "&redirect_uri=" + URLEncoder.encode(MONZO_URI_AUTH_CALLBACK)
                + "&response_type=code")
        Logger.d("CanIBuyIt", url)
        webview.loadUrl(url)
    }

    override fun inject(): BasePresenter<Screen>? {
        Injector.INSTANCE.graph.inject(this)
        return null
    }

    companion object {

        fun show(context: Context) {
            val intent = Intent(context, LoginActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
}
