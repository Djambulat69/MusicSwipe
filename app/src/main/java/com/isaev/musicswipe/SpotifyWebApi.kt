package com.isaev.musicswipe

import retrofit2.http.GET
import retrofit2.http.Query

interface SpotifyWebApi {

    @GET("recommendations")
    suspend fun getRecommendations(
        @Query("seed_artists") seedArtists: String,
        @Query("seed_genres") seedGenres: String,
        @Query("seed_tracks") seedTracks: String,
        @Query("limit") limit: Int
    ): RecommendationsResponse
}
