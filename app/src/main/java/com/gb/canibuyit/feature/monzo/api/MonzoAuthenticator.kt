package com.gb.canibuyit.feature.monzo.api

import com.gb.canibuyit.feature.monzo.CLIENT_ID
import com.gb.canibuyit.feature.monzo.CLIENT_SECRET
import com.gb.canibuyit.feature.monzo.CredentialsProvider
import com.gb.canibuyit.feature.monzo.data.MonzoMapper
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject

class MonzoAuthenticator @Inject constructor(private val monzoAuthApi: MonzoAuthApi,
                                             private val credentialsProvider: CredentialsProvider,
                                             private val monzoMapper: MonzoMapper
) : Authenticator, Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        return chain.proceed(chain.request().newBuilder()
            .addHeader(AUTHORIZATION, HEADER_VALUE_PREFIX + credentialsProvider.accessToken)
            .build())
    }

    override fun authenticate(route: Route?, response: Response): Request? {
        val login = monzoAuthApi.refresh(
            grantType = "refresh_token",
            refreshToken = credentialsProvider.refreshToken ?: "",
            clientId = CLIENT_ID,
            clientSecret = CLIENT_SECRET).map { monzoMapper.mapToLogin(it) }.blockingGet()
        credentialsProvider.accessToken = login.accessToken
        credentialsProvider.refreshToken = login.refreshToken
        credentialsProvider.accessTokenExpiry = login.expiresAt
        return response.request.newBuilder()
            .removeHeader(AUTHORIZATION)
            .addHeader(AUTHORIZATION, HEADER_VALUE_PREFIX + credentialsProvider.accessToken)
            .build()
    }
}

private const val AUTHORIZATION = "Authorization"
private const val HEADER_VALUE_PREFIX = "Bearer "
