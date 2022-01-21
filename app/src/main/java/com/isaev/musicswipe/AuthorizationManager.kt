package com.isaev.musicswipe

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import androidx.core.content.edit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.create
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST


object AuthorizationManager {

    private const val TAG = "AuthorizationManager"

    private const val BASE_URL = "https://accounts.spotify.com/"
    private const val TOKEN_KEY = "token_key"
    private const val REFRESH_TOKEN_KEY = "refresh_token_key"

    private const val CLIENT_ID = "ff565d0979aa4da5810b5f3d55057c8f"
    private const val REDIRECT_URI = "http://music.swipe.com"

    private lateinit var codeVerifier: String
    private val codeChallenge: String by lazy { Pkce.generateCodeChallenge(codeVerifier) }

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(
            OkHttpClient.Builder()
                .addInterceptor(
                    HttpLoggingInterceptor { message -> Log.i(TAG, message) }
                        .apply { level = HttpLoggingInterceptor.Level.BODY }
                )
                .build()
        )
        .addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))
        .build()

    private val authService = retrofit.create<SpotifyAuthService>()

    private val _token: MutableStateFlow<String?> = MutableStateFlow(null)
    private var refreshToken: String? = null

    val token: String get() = _token.value!!
    val authState: Flow<Boolean> = _token.map { it != null }

    suspend fun initTokens() {
        val prefs = getPrefs()
        val savedToken = prefs.getString(TOKEN_KEY, null) ?: return
        val savedRefreshToken = prefs.getString(REFRESH_TOKEN_KEY, null) ?: return
        _token.value = savedToken
        refreshToken = savedRefreshToken
        refreshTokens()
    }

    fun isAuthorized(): Boolean = _token.value != null

    // --- Auth ---
    suspend fun authorizeUrl(): String {
        if (!::codeVerifier.isInitialized) {
            codeVerifier = Pkce.generateCodeVerifier()
        }

        return Uri.parse("https://accounts.spotify.com/")
            .buildUpon()
            .appendPath("authorize")
            .appendQueryParameter("show_dialog", "true")
            .appendQueryParameter("client_id", "ff565d0979aa4da5810b5f3d55057c8f")
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("redirect_uri", "http://music.swipe.com")
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

    private suspend fun refreshTokens() {
        refreshToken?.let {
            val response = authService.refreshToken("refresh_token", it, CLIENT_ID)
            _token.value = response.accessToken
            this.refreshToken = response.refreshToken
            getPrefs().edit {
                putString(TOKEN_KEY, response.accessToken)
                putString(REFRESH_TOKEN_KEY, response.refreshToken)
            }
        }
    }

    private suspend fun requestToken(code: String) =
        authService.requestToken(
            "authorization_code", code, REDIRECT_URI, CLIENT_ID, codeVerifier
        )

    private fun getPrefs(): SharedPreferences {
        return MusicSwipeApp.instance.getSharedPreferences("auth", Context.MODE_PRIVATE)
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
