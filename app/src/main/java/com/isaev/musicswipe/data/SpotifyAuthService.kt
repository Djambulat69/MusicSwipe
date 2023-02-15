package com.isaev.musicswipe.data

import android.content.SharedPreferences
import android.net.Uri
import androidx.core.content.edit
import com.isaev.musicswipe.Pkce
import com.isaev.musicswipe.Token
import com.isaev.musicswipe.di.UserModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton


@Singleton
class SpotifyAuthService @Inject constructor(
    private val authApi: SpotifyAuthApi,
    @Named(UserModule.USER_PREFS_NAME) private val userPrefs: SharedPreferences,
    private val externalScope: CoroutineScope
) {

    private lateinit var codeVerifier: String
    private val codeChallenge: String by lazy { Pkce.generateCodeChallenge(codeVerifier) }

    private val _token: MutableStateFlow<Token?> = MutableStateFlow(null)
    private var refreshToken: String? = null

    val token: String get() = ((_token.value!!) as Token.AccessToken).token
    val authState: StateFlow<Token?> = _token.asStateFlow()

    init {
        externalScope.launch {
            refreshTokens()
        }
    }

    fun isAuthorized(): Boolean = _token.value is Token.AccessToken

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

    fun getAuthRequest(): AuthorizationRequest {
        val config = AuthorizationServiceConfiguration(
            Uri.parse("${BASE_URL}authorize"),
            Uri.parse("${BASE_URL}api/token")
        )

        val authRequest = AuthorizationRequest.Builder(
            config,
            CLIENT_ID,
            ResponseTypeValues.CODE,
            Uri.parse(REDIRECT_URI)
        ).setScope("user-top-read user-read-private")
            .build()

        return authRequest
    }

    suspend fun authorize(authCode: String) {
        val response = authApi.requestToken(
            "authorization_code", authCode, REDIRECT_URI, CLIENT_ID, codeVerifier
        )

        _token.update { Token.AccessToken(response.accessToken) }

        refreshToken = response.refreshToken

        userPrefs.edit {
            putString(TOKEN_KEY, response.accessToken)
            putString(REFRESH_TOKEN_KEY, response.refreshToken)
        }
    }

    fun logOut() {
        _token.value = Token.EmptyToken
        userPrefs.edit {
            remove(TOKEN_KEY)
            remove(REFRESH_TOKEN_KEY)
        }
    }

    private suspend fun refreshTokens() {
        val savedRefreshToken = userPrefs.getString(REFRESH_TOKEN_KEY, null)

        if (savedRefreshToken == null) {
            _token.update { Token.EmptyToken }
        } else {
            val response = authApi.refreshToken("refresh_token", savedRefreshToken, CLIENT_ID)

            _token.update { Token.AccessToken(response.accessToken) }

            userPrefs.edit {
                putString(TOKEN_KEY, response.accessToken)
                putString(REFRESH_TOKEN_KEY, response.refreshToken)
            }
        }
    }

    companion object {
        const val TAG = "UserRepository"

        private const val BASE_URL = "https://accounts.spotify.com/"
        private const val TOKEN_KEY = "token_key"
        private const val REFRESH_TOKEN_KEY = "refresh_token_key"

        private const val CLIENT_ID = "ff565d0979aa4da5810b5f3d55057c8f"
        private const val REDIRECT_URI = "com.music.swipe://login"
    }
}

interface SpotifyAuthApi {

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
