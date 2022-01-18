package com.isaev.musicswipe

import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AuthorizationManager {

    private val _toketState: MutableStateFlow<String?> = MutableStateFlow(null)
    private val _authState: MutableStateFlow<Boolean> = MutableStateFlow(false)

    val token: String get() = _toketState.value!!
    val authState: StateFlow<Boolean> = _authState.asStateFlow()

    fun isAuthorized(): Boolean = _toketState.value != null

    fun request(): AuthorizationRequest {
        return AuthorizationRequest.Builder(
            CLIENT_ID, AuthorizationResponse.Type.TOKEN, REDIRECT_URI
        ).apply {
            setScopes(arrayOf("user-top-read"))
        }.build()
    }

    suspend fun setToken(newToken: String) {
        _toketState.emit(newToken)
        _authState.emit(true)
    }

    private const val CLIENT_ID = "ff565d0979aa4da5810b5f3d55057c8f"
    private const val REDIRECT_URI = "http://music.swipe.com"
}
