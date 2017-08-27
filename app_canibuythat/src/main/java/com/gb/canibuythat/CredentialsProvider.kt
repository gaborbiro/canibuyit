package com.gb.canibuythat

import com.gb.canibuythat.util.PrefsUtil

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CredentialsProvider @Inject constructor(private val prefsUtil: PrefsUtil) {

    var accessToken: String?
        get() = prefsUtil.get(PREF_ACCESS_TOKEN, "")
        set(accessToken) = prefsUtil.put(PREF_ACCESS_TOKEN, accessToken)

    var refreshToken: String?
        get() = prefsUtil.get(PREF_REFRESH_TOKEN, null as String?)
        set(refreshToken) = prefsUtil.put(PREF_REFRESH_TOKEN, refreshToken)

    companion object {
        private val PREF_ACCESS_TOKEN = "com.gb.canibuythat.CredentialsProvider.PREF_ACCESS_TOKEN"
        private val PREF_REFRESH_TOKEN = "com.gb.canibuythat.CredentialsProvider.PREF_REFRESH_TOKEN"
    }
}
