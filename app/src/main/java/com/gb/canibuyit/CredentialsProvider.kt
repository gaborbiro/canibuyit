package com.gb.canibuyit

import com.gb.canibuyit.util.PrefsUtil

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CredentialsProvider @Inject constructor(private val prefsUtil: PrefsUtil) {

    var accessToken: String?
        get() = prefsUtil.get(PREF_ACCESS_TOKEN, "")
        set(accessToken) = prefsUtil.put(PREF_ACCESS_TOKEN, accessToken)

    var accessTokenExpiry: Long
        get() = prefsUtil.get(PREF_ACCESS_TOKEN_EXPIRY, Long.MAX_VALUE)
        set(value) = prefsUtil.put(PREF_ACCESS_TOKEN_EXPIRY, value)

    var refreshToken: String?
        get() = prefsUtil.get(PREF_REFRESH_TOKEN, null as String?)
        set(refreshToken) = prefsUtil.put(PREF_REFRESH_TOKEN, refreshToken)

    fun isRefresh(): Boolean {
        return !refreshToken.isNullOrEmpty()
    }

    companion object {
        private val PREF_ACCESS_TOKEN = "com.gb.canibuyit.CredentialsProvider.PREF_ACCESS_TOKEN"
        private val PREF_REFRESH_TOKEN = "com.gb.canibuyit.CredentialsProvider.PREF_REFRESH_TOKEN"
        private val PREF_ACCESS_TOKEN_EXPIRY = "com.gb.canibuyit.CredentialsProvider.PREF_ACCESS_TOKEN_EXPIRY"
    }
}