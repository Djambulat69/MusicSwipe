package com.isaev.musicswipe.data

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface SpotifyWebApi {

    @GET("recommendations")
    suspend fun getRecommendations(
        @Query("seed_artists") seedArtists: String,
        @Query("seed_genres") seedGenres: String,
        @Query("seed_tracks") seedTracks: String,
        @Query("limit") limit: Int
    ): RecommendationsResponse

    @GET("artists/{id}")
    suspend fun getArtist(
        @Path("id") id: String
    ): ArtistResponse

    @GET("me/top/tracks")
    suspend fun getTopTracks(
        @Query("limit") limit: Int
    ): TopTracksResponse

    @GET("me")
    suspend fun getMe(): User
}
