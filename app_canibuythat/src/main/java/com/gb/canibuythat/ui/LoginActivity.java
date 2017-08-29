package com.gb.canibuythat.ui;

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
import android.widget.Toast;

import com.gb.canibuythat.CredentialsProvider;
import com.gb.canibuythat.MonzoConstants;
import com.gb.canibuythat.R;
import com.gb.canibuythat.di.Injector;
import com.gb.canibuythat.util.Logger;

import java.net.URLEncoder;

import javax.inject.Inject;

import butterknife.BindView;

public class LoginActivity extends BaseActivity {

    @Inject CredentialsProvider credentialsProvider;
    @BindView(R.id.webview) WebView webView;

    public static void show(Context context) {
        Intent i = new Intent(context, LoginActivity.class);
        context.startActivity(i);
    }

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

        String url = MonzoConstants.MONZO_OAUTH_URL
                + "/?client_id=" + MonzoConstants.CLIENT_ID
                + "&redirect_uri=" + URLEncoder.encode(MonzoConstants.MONZO_URI_AUTH_CALLBACK)
                + "&response_type=code";
        Logger.INSTANCE.d("CanIBuyThat", url);
        webView.loadUrl(url);
    }

    @Override
    protected void inject() {
        Injector.INSTANCE.getGraph().inject(this);
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
