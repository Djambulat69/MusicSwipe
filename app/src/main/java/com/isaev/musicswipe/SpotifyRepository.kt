package com.isaev.musicswipe

import javax.inject.Inject


class SpotifyRepository @Inject constructor(
    private val spotifyWebApiService: SpotifyWebApi
) {

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

    companion object {
        const val TAG = "SpotifyRepository"
    }
}
