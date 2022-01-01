package com.isaev.musicswipe

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit

class SpotifyWebApiHelper(token: String) {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(
            OkHttpClient.Builder()
                .addInterceptor(AuthInterceptor(token))
                .build()
        )
        .addConverterFactory(json.asConverterFactory(MediaType.parse("application/json")!!))
        .build()

    private val spotifyWebApiService = retrofit.create(SpotifyWebApi::class.java)

    suspend fun getRecommendations(
        seedArtists: Array<String>,
        seedGenres: Array<String>,
        seedTracks: Array<String>,
        limit: Int
    ): RecommendationsResponse =
        spotifyWebApiService.getRecommendations(
            seedArtists.joinToString(separator = ","),
            seedGenres.joinToString(separator = ","),
            seedTracks.joinToString(separator = ","),
            limit
        )

    companion object {
        private const val BASE_URL = "https://api.spotify.com/v1/"
    }
}
