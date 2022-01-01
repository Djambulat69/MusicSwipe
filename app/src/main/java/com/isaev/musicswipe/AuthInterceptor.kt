package com.isaev.musicswipe

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val token: String) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder().addHeader(AUTH_HEADER, "Bearer $token").build()
        return chain.proceed(request)
    }

    companion object {
        private const val AUTH_HEADER = "Authorization"
    }

}
