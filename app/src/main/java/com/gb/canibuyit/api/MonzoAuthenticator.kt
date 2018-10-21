package com.gb.canibuyit.api

import com.gb.canibuyit.CLIENT_ID
import com.gb.canibuyit.CLIENT_SECRET
import com.gb.canibuyit.CredentialsProvider
import com.gb.canibuyit.repository.MonzoMapper
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject

class MonzoAuthenticator @Inject constructor(val monzoAuthApi: MonzoAuthApi,
                                             val credentialsProvider: CredentialsProvider,
                                             val monzoMapper: MonzoMapper) : Authenticator, Interceptor {
    companion object {
        val AUTHORIZATION = "Authorization"
        val HEADER_VALUE_PREFIX = "Bearer "
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        return chain.proceed(chain.request().newBuilder()
                .addHeader(AUTHORIZATION, HEADER_VALUE_PREFIX + credentialsProvider.accessToken)
                .build())
    }

    override fun authenticate(route: Route?, response: Response?): Request? {
        val login = monzoAuthApi.refresh(
                grantType = "refresh_token",
                refreshToken = credentialsProvider.refreshToken ?: "",
                clientId = CLIENT_ID,
                clientSecret = CLIENT_SECRET).map { monzoMapper.mapToLogin(it) }.blockingGet()
        credentialsProvider.accessToken = login.accessToken
        credentialsProvider.refreshToken = login.refreshToken
        credentialsProvider.accessTokenExpiry = login.expiresAt
        return response?.request()?.newBuilder()
                ?.removeHeader(AUTHORIZATION)
                ?.addHeader(AUTHORIZATION, HEADER_VALUE_PREFIX + credentialsProvider.accessToken)
                ?.build()
    }
}