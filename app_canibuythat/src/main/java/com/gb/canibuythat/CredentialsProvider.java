package com.gb.canibuythat;

import com.gb.canibuythat.util.PrefsUtil;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class CredentialsProvider {

    private static final String PREF_ACCESS_TOKEN = "com.gb.canibuythat.CredentialsProvider.PREF_ACCESS_TOKEN";
    private static final String PREF_REFRESH_TOKEN = "com.gb.canibuythat.CredentialsProvider.PREF_REFRESH_TOKEN";

    private PrefsUtil prefsUtil;

    @Inject
    public CredentialsProvider(PrefsUtil prefsUtil) {
        this.prefsUtil = prefsUtil;
    }

    public void setAccessToken(String accessToken) {
        prefsUtil.put(PREF_ACCESS_TOKEN, accessToken);
    }

    public String getAccessToken() {
        return prefsUtil.get(PREF_ACCESS_TOKEN, (String) null);
    }

    public void setRefreshToken(String refreshToken) {
        prefsUtil.put(PREF_REFRESH_TOKEN, refreshToken);
    }

    public String getRefreshToken() {
        return prefsUtil.get(PREF_REFRESH_TOKEN, (String) null);
    }
}
