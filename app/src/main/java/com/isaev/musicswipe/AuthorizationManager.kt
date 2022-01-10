package com.isaev.musicswipe

import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse

object AuthorizationManager {

    fun request(): AuthorizationRequest {
        return AuthorizationRequest.Builder(
            CLIENT_ID, AuthorizationResponse.Type.TOKEN, REDIRECT_URI
        ).apply {
            setScopes(arrayOf("user-library-modify", "user-library-read", "user-top-read"))
        }.build()
    }

    private const val CLIENT_ID = "ff565d0979aa4da5810b5f3d55057c8f"
    private const val REDIRECT_URI = "http://music.swipe.com"
}
