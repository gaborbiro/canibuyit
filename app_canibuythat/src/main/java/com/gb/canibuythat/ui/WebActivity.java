package com.gb.canibuythat.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.gb.canibuythat.R;

import butterknife.BindView;

public class WebActivity extends BaseActivity {

    private static final String EXTRA_URL = "com.gb.canibuythat.ui.WebActivity.EXTRA_URL";

    @BindView(R.id.webview) WebView webView;

    public static void show(Context context, String url) {
        Intent i = new Intent(context, WebActivity.class);
        i.putExtra(EXTRA_URL, url);
        context.startActivity(i);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web);

        webView.setWebChromeClient(new WebChromeClient());
        webView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        webView.setScrollbarFadingEnabled(true);
        webView.setWebViewClient(new MyWebViewClient());

        if (getIntent().getStringExtra(EXTRA_URL) != null) {
            webView.loadUrl(getIntent().getStringExtra(EXTRA_URL));
        }
    }

    @Override
    protected void inject() {
    }

    private class MyWebViewClient extends WebViewClient {

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            Toast.makeText(view.getContext(), url + " loading", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            Toast.makeText(view.getContext(), url + " finished", Toast.LENGTH_SHORT).show();
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
