package com.isaev.musicswipe

import android.util.Log
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.create


object SpotifyWebApiHelper {

    private const val TAG = "SpotifyWebApiHelper"
    private const val BASE_URL = "https://api.spotify.com/v1/"

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(
            OkHttpClient.Builder()
                .addInterceptor(AuthInterceptor())
                .addInterceptor(
                    HttpLoggingInterceptor { message ->
                        Log.i(TAG, message)
                    }.apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    }
                )
                .build()
        )
        .addConverterFactory(json.asConverterFactory(("application/json").toMediaType()))
        .build()

    private val spotifyWebApiService = retrofit.create<SpotifyWebApi>()

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

    suspend fun getArtist(id: String): ArtistResponse = spotifyWebApiService.getArtist(id)

    suspend fun getTopTracks(limit: Int): TopTracksResponse = spotifyWebApiService.getTopTracks(limit)

    suspend fun getMe(): User = spotifyWebApiService.getMe()
}
