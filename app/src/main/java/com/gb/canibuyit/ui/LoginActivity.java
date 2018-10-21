package com.gb.canibuyit.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.gb.canibuyit.CredentialsProvider;
import com.gb.canibuyit.R;
import com.gb.canibuyit.di.Injector;
import com.gb.canibuyit.presenter.BasePresenter;
import com.gb.canibuyit.screen.Screen;
import com.gb.canibuyit.util.Logger;

import java.net.URLEncoder;

import javax.inject.Inject;

import butterknife.BindView;

import static com.gb.canibuyit.MonzoConstantsKt.CLIENT_ID;
import static com.gb.canibuyit.MonzoConstantsKt.MONZO_OAUTH_URL;
import static com.gb.canibuyit.MonzoConstantsKt.MONZO_URI_AUTH_CALLBACK;

public class LoginActivity extends BaseActivity {

    @Inject CredentialsProvider credentialsProvider;
    @BindView(R.id.webview) WebView webView;

    public static void show(Context context) {
        Intent i = new Intent(context, LoginActivity.class);
        context.startActivity(i);
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web);

        credentialsProvider.setAccessToken(null);

        webView.setWebChromeClient(new WebChromeClient());
        webView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        webView.setScrollbarFadingEnabled(false);
        webView.setWebViewClient(new MyWebViewClient());
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        String url = MONZO_OAUTH_URL
                + "/?client_id=" + CLIENT_ID
                + "&redirect_uri=" + URLEncoder.encode(MONZO_URI_AUTH_CALLBACK)
                + "&response_type=code";
        Logger.INSTANCE.d("CanIBuyIt", url);
        webView.loadUrl(url);
    }

    @Override
    protected BasePresenter<Screen> inject() {
        Injector.INSTANCE.getGraph().inject(this);
        return null;
    }

    private class MyWebViewClient extends WebViewClient {

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            return super.shouldOverrideUrlLoading(view, request);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return super.shouldOverrideUrlLoading(view, url);
        }
    }
}
