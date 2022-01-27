package com.isaev.musicswipe.data

import com.isaev.musicswipe.Token
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject


class SpotifyRepository @Inject constructor(
    private val spotifyRemote: SpotifyRemoteDataSource,
    private val spotifyAuthService: SpotifyAuthService
) {

    val authState: StateFlow<Token?> = spotifyAuthService.authState

    suspend fun getRecommendations(
        seedArtists: Array<String>,
        seedGenres: Array<String>,
        seedTracks: Array<String>,
        limit: Int
    ): List<Track> =
        spotifyRemote.getRecommendations(
            seedArtists,
            seedGenres,
            seedTracks,
            limit
        ).filter { it.previewUrl != null }

    suspend fun getArtistTopGenre(artistId: String): String? = spotifyRemote.getArtist(artistId).genres.firstOrNull()

    suspend fun getTopTracks(limit: Int): List<Track> = spotifyRemote.getTopTracks(limit)

    suspend fun getMe(): User = spotifyRemote.getMe()

    companion object {
        const val TAG = "SpotifyRepository"
    }
}
