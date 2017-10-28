package com.gb.canibuythat.api

import com.gb.canibuythat.CredentialsProvider
import com.gb.canibuythat.MonzoConstants
import com.gb.canibuythat.repository.MonzoMapper
import okhttp3.*
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
                clientId = MonzoConstants.CLIENT_ID,
                clientSecret = MonzoConstants.CLIENT_SECRET).map { monzoMapper.mapToLogin(it) }.blockingGet()
        credentialsProvider.accessToken = login.accessToken
        credentialsProvider.refreshToken = login.refreshToken
        credentialsProvider.accessTokenExpiry = login.expiresAt
        return response?.request()?.newBuilder()
                ?.removeHeader(AUTHORIZATION)
                ?.addHeader(AUTHORIZATION, HEADER_VALUE_PREFIX + credentialsProvider.accessToken)
                ?.build()
    }
}