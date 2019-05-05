package com.gb.canibuyit.feature.monzo

import com.gb.prefsutil.PrefsUtil
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CredentialsProvider @Inject constructor(private val prefsUtil: PrefsUtil) {

    var accessToken: String?
        get() = prefsUtil[PREF_ACCESS_TOKEN, ""]
        set(accessToken) = accessToken?.let { prefsUtil.put(PREF_ACCESS_TOKEN, accessToken) }
            ?: run {
                prefsUtil.remove(
                    PREF_ACCESS_TOKEN)
            }

    var accessTokenExpiry: Long
        get() = prefsUtil[PREF_ACCESS_TOKEN_EXPIRY, Long.MAX_VALUE]
        set(value) = prefsUtil.put(PREF_ACCESS_TOKEN_EXPIRY, value)

    var refreshToken: String?
        get() = prefsUtil[PREF_REFRESH_TOKEN, ""]
        set(refreshToken) = refreshToken?.let { prefsUtil.put(PREF_REFRESH_TOKEN, refreshToken) }
            ?: run {
                prefsUtil.remove(
                    PREF_REFRESH_TOKEN)
            }

    fun isRefreshToken(): Boolean {
        return !refreshToken.isNullOrEmpty()
    }
}

private const val PREF_ACCESS_TOKEN =
    "com.gb.canibuyit.feature.monzo.CredentialsProvider.PREF_ACCESS_TOKEN"
private const val PREF_REFRESH_TOKEN =
    "com.gb.canibuyit.feature.monzo.CredentialsProvider.PREF_REFRESH_TOKEN"
private const val PREF_ACCESS_TOKEN_EXPIRY =
    "com.gb.canibuyit.feature.monzo.CredentialsProvider.PREF_ACCESS_TOKEN_EXPIRY"
