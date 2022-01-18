package com.isaev.musicswipe

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor() : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response =
        if (AuthorizationManager.isAuthorized()) {
            val token = AuthorizationManager.token
            val request = chain.request().newBuilder().addHeader(AUTH_HEADER, "Bearer $token").build()
            chain.proceed(request)
        } else {
            chain.proceed(chain.request())
        }


    companion object {
        private const val AUTH_HEADER = "Authorization"
    }

}
