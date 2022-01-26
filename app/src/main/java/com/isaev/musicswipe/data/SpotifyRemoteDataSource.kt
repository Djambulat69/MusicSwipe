package com.isaev.musicswipe.data

import javax.inject.Inject

class SpotifyRemoteDataSource @Inject constructor(
    private val spotifyWebApi: SpotifyWebApi
) {
    suspend fun getRecommendations(
        seedArtists: Array<String>,
        seedGenres: Array<String>,
        seedTracks: Array<String>,
        limit: Int
    ): List<Track> =
        spotifyWebApi.getRecommendations(
            seedArtists.joinToString(separator = ","),
            seedGenres.joinToString(separator = ","),
            seedTracks.joinToString(separator = ","),
            limit
        ).tracks

    suspend fun getArtist(artistId: String): ArtistResponse = spotifyWebApi.getArtist(artistId)

    suspend fun getTopTracks(limit: Int): List<Track> = spotifyWebApi.getTopTracks(limit).items

    suspend fun getMe(): User = spotifyWebApi.getMe()
}
