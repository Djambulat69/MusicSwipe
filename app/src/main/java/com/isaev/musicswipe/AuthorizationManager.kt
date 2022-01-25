package com.isaev.musicswipe

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.core.content.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class AuthorizationManager @Inject constructor(
    private val authService: SpotifyAuthService
) {

    private lateinit var codeVerifier: String
    private val codeChallenge: String by lazy { Pkce.generateCodeChallenge(codeVerifier) }

    private val _token: MutableStateFlow<String?> = MutableStateFlow(null)
    private var refreshToken: String? = null

    val token: String get() = _token.value!!
    val authState: Flow<Boolean> = _token.map { it != null }

    fun isAuthorized(): Boolean = _token.value != null

    suspend fun authorizeUrl(): String {
        if (!::codeVerifier.isInitialized) {
            codeVerifier = Pkce.generateCodeVerifier()
        }

        return Uri.parse(BASE_URL)
            .buildUpon()
            .appendPath("authorize")
            .appendQueryParameter("show_dialog", "true")
            .appendQueryParameter("client_id", CLIENT_ID)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("redirect_uri", REDIRECT_URI)
            .appendQueryParameter("state", "auth")
            .appendQueryParameter("scope", "user-top-read user-read-private")
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("code_challenge", codeChallenge)
            .build()
            .toString()
    }

    suspend fun authorize(authCode: String) {
        val response = requestToken(authCode)
        _token.value = response.accessToken
        refreshToken = response.refreshToken

        getPrefs().edit {
            putString(TOKEN_KEY, response.accessToken)
            putString(REFRESH_TOKEN_KEY, response.refreshToken)
        }
    }

    suspend fun refreshTokens() {
        val prefs = getPrefs()
        val savedRefreshToken = prefs.getString(REFRESH_TOKEN_KEY, null) ?: return

        val response = authService.refreshToken("refresh_token", savedRefreshToken, CLIENT_ID)

        _token.value = response.accessToken

        getPrefs().edit {
            putString(TOKEN_KEY, response.accessToken)
            putString(REFRESH_TOKEN_KEY, response.refreshToken)
        }
    }

    private suspend fun requestToken(code: String) =
        authService.requestToken(
            "authorization_code", code, REDIRECT_URI, CLIENT_ID, codeVerifier
        )

    private fun getPrefs(): SharedPreferences {
        return MusicSwipeApp.instance.getSharedPreferences("auth", Context.MODE_PRIVATE)
    }

    companion object {
        const val TAG = "AuthorizationManager"

        private const val BASE_URL = "https://accounts.spotify.com/"
        private const val TOKEN_KEY = "token_key"
        private const val REFRESH_TOKEN_KEY = "refresh_token_key"

        private const val CLIENT_ID = "ff565d0979aa4da5810b5f3d55057c8f"
        private const val REDIRECT_URI = "http://music.swipe.com"
    }
}

interface SpotifyAuthService {

    @FormUrlEncoded
    @POST("api/token")
    suspend fun requestToken(
        @Field("grant_type") grantType: String,
        @Field("code") code: String,
        @Field("redirect_uri") redirectUri: String,
        @Field("client_id") clientId: String,
        @Field("code_verifier") codeVerifier: String
    ): AuthTokenResponse

    @FormUrlEncoded
    @POST("api/token")
    suspend fun refreshToken(
        @Field("grant_type") grantType: String,
        @Field("refresh_token") refreshToken: String,
        @Field("client_id") clientId: String
    ): AuthTokenResponse
}

@Serializable
data class AuthTokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String,
    @SerialName("scope") val scope: String,
    @SerialName("expires_in") val expiresIn: Int,
    @SerialName("refresh_token") val refreshToken: String?
)
