package com.isaev.musicswipe

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val userRepository: UserRepository
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response =
        if (userRepository.isAuthorized()) {
            val token = userRepository.token
            val request = chain.request().newBuilder().addHeader(AUTH_HEADER, "Bearer $token").build()
            chain.proceed(request)
        } else {
            chain.proceed(chain.request())
        }


    companion object {
        private const val AUTH_HEADER = "Authorization"
    }

}
