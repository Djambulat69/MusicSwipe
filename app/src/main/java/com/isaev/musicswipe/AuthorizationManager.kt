package com.isaev.musicswipe

import android.net.Uri
import android.util.Log
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.*


object AuthorizationManager {

    private const val TAG = "AuthorizationManager"

    private const val CLIENT_ID = "ff565d0979aa4da5810b5f3d55057c8f"
    private const val REDIRECT_URI = "http://music.swipe.com"

    private lateinit var CODE_VERIFIER: String
    private val CODE_CHALLENGE: String by lazy { Pkce.generateCodeChallenge(CODE_VERIFIER) }

    private const val BASE_URL = "https://accounts.spotify.com/"

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
        .addConverterFactory(ScalarsConverterFactory.create())
        .addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))
        .build()

    private val authService = retrofit.create(SpotifyAuthService::class.java)

    private val _toketState: MutableStateFlow<String?> = MutableStateFlow(null)
    private val _authState: MutableStateFlow<Boolean> = MutableStateFlow(false)

    val token: String get() = _toketState.value!!
    var refreshToken: String? = null
    val authState: StateFlow<Boolean> = _authState.asStateFlow()

    fun isAuthorized(): Boolean = _toketState.value != null

    fun setToken(newToken: String) {
        _toketState.value = newToken
        _authState.value = true
    }


    // AuthService methods

    suspend fun authorizeUrl(): String {
        CODE_VERIFIER = Pkce.generateCodeVerifier()

        return Uri.parse("https://accounts.spotify.com/")
            .buildUpon()
            .appendPath("authorize")
            .appendQueryParameter("show_dialog", "true")
            .appendQueryParameter("client_id", "ff565d0979aa4da5810b5f3d55057c8f")
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("redirect_uri", "http://music.swipe.com")
            .appendQueryParameter("state", "auth")
            .appendQueryParameter("scope", "user-top-read")
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("code_challenge", CODE_CHALLENGE)
            .build()
            .toString()
    }

    suspend fun authorize(authCode: String) {
        val response = requestToken(authCode)
        setToken(response.accessToken)
        refreshToken = response.refreshToken
    }

    suspend fun refreshToken(refreshToken: String) {
        val response = authService.refreshToken("refresh_token", refreshToken, CLIENT_ID)
        setToken(response.accessToken)
        this.refreshToken = refreshToken
    }

    private suspend fun requestToken(code: String) =
        authService.requestToken(
            "authorization_code", code, REDIRECT_URI, CLIENT_ID, CODE_VERIFIER
        )
}

interface SpotifyAuthService {

    @GET("authorize")
    suspend fun authorize(
        @Query("client_id") clientId: String,
        @Query("response_type") responseType: String,
        @Query("redirect_uri") redirectUri: String,
        @Query("state") state: String,
        @Query("scope") scope: String,
        @Query("code_challenge_method") codeChallengeMethod: String,
        @Query("code_challenge") codeChallenge: String
    ): String

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
data class AuthorizeResponse(
    @SerialName("code") val code: String,
    @SerialName("state") val state: String
)

@Serializable
data class AuthTokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String,
    @SerialName("scope") val scope: String,
    @SerialName("expires_in") val expiresIn: Int,
    @SerialName("refresh_token") val refreshToken: String?
)
