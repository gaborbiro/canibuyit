package com.gb.canibuyit.feature.monzo

import com.gb.prefsutil.PrefsUtil
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CredentialsProvider @Inject constructor(prefsUtil: PrefsUtil) {

    var accessToken: String? by prefsUtil.delegate(PREF_ACCESS_TOKEN)

    var accessTokenExpiry: Long by prefsUtil.delegate(PREF_ACCESS_TOKEN_EXPIRY, Long.MAX_VALUE)

    var refreshToken: String? by prefsUtil.delegate(PREF_REFRESH_TOKEN)

    fun isRefreshToken(): Boolean {
        return !refreshToken.isNullOrEmpty()
    }
}

private const val PREF_ACCESS_TOKEN = "com.gb.canibuyit.feature.monzo.CredentialsProvider.PREF_ACCESS_TOKEN"
private const val PREF_REFRESH_TOKEN = "com.gb.canibuyit.feature.monzo.CredentialsProvider.PREF_REFRESH_TOKEN"
private const val PREF_ACCESS_TOKEN_EXPIRY = "com.gb.canibuyit.feature.monzo.CredentialsProvider.PREF_ACCESS_TOKEN_EXPIRY"
