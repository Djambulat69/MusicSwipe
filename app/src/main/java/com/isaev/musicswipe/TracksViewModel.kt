package com.isaev.musicswipe

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.ArrayList

class TracksViewModel : ViewModel() {

    private var spotifyWebApiHelper: SpotifyWebApiHelper? = null

    private val _tracks = MutableLiveData<List<PlayTrack>>()
    private val _playback = MutableLiveData<Boolean>(false)

    @Volatile
    private var isLoadingRecommendations = false

    @Volatile
    private var isPreparing = false

    private val mediaPlayer = MediaPlayer().apply {
        setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build()
        )
        setOnCompletionListener { mediaPlayer: MediaPlayer ->
            mediaPlayer.start()
        }
    }

    private val trackSeeds: Queue<String> = ArrayDeque<String>(
        listOf("5KmJMi7MiBJtCeJ08lmKzo", "1RUubW9fHtIYwjl588PrhZ", "6xATKzdUO89mZvStODhUSR")
    )
    private var artistSeed = "2RdwBSPQiwcmiDo9kixcl8"
    private var genreSeed = "pop"

    var token: String? = null
        set(newToken) {
            field = newToken
            newToken?.let {
                spotifyWebApiHelper = SpotifyWebApiHelper(newToken)
            }

            viewModelScope.launch {
                loadTopTracksSeeds()
                loadRecommendations()
            }
        }

    val tracks: LiveData<List<PlayTrack>> = _tracks
    val playback: LiveData<Boolean> = _playback


    override fun onCleared() {
        mediaPlayer.release()
    }

    fun loadRecommendations(limit: Int = 10) {
        if (isLoadingRecommendations) return
        isLoadingRecommendations = true
        viewModelScope.launch {
            try {
                val recommendationsResponse = spotifyWebApiHelper?.getRecommendations(
                    arrayOf(artistSeed),
                    arrayOf(genreSeed),
                    trackSeeds.toTypedArray(),
                    limit
                )

                recommendationsResponse?.let {
                    val newTracks =
                        recommendationsResponse.tracks.filter { it.previewUrl != null }.map { PlayTrack(it) }

                    _tracks.value = _tracks.value?.plus(newTracks) ?: newTracks
                    val names = recommendationsResponse.tracks.map { it.name }
                    Log.i(TAG, names.toString())
                }
            } catch (e: Exception) {
                Log.i(TAG, e.stackTraceToString())
            } finally {
                isLoadingRecommendations = false
            }
        }
    }


    fun prepareNewTrack(url: String, position: Int) {
        isPreparing = true
        _playback.value = false
        with(mediaPlayer) {
            reset()
            setDataSource(url)
            prepareAsync()
            setOnPreparedListener {
                isPreparing = false
                onTrackPlayClicked(position)
            }
        }
    }

    fun onTrackPlayClicked(position: Int) {
        if (isPreparing) return

        val currentTracks = _tracks.value

        if (currentTracks != null) {
            onTrackPlayClicked(currentTracks[position])
        }
    }

    fun onTrackPlayClicked(playTrack: PlayTrack) {
        if (isPreparing) return

        val isPlaying = playTrack.isPlaying

        if (isPlaying) mediaPlayer.pause() else mediaPlayer.start()
        playTrack.isPlaying = !isPlaying

        _playback.value = playTrack.isPlaying
    }

    fun onTrackLiked(playTrack: PlayTrack) {
        trackSeeds.poll()
        trackSeeds.offer(playTrack.track.id)
        val mainArtistId = playTrack.track.artists.first().id
        artistSeed = mainArtistId
        viewModelScope.launch {
            try {
                val newGenreSeed: String =
                    spotifyWebApiHelper?.getArtist(mainArtistId)?.genres?.firstOrNull() ?: return@launch
                genreSeed = newGenreSeed
                loadRecommendations(5)
            } catch (e: Throwable) {
                Log.i(TAG, "onTrackLiked: ${e.stackTraceToString()}")
            }
        }
    }

    private suspend fun loadTopTracksSeeds() {
        val topTracksResponse = spotifyWebApiHelper?.getTopTracks(3) ?: return
        val topTracks = topTracksResponse.items

        trackSeeds.clear()
        trackSeeds.addAll(topTracks.map { it.id })

        val topMainArtistId = topTracks.first().artists.first().id

        artistSeed = topMainArtistId

        val newGenreSeed: String = spotifyWebApiHelper?.getArtist(topMainArtistId)?.genres?.firstOrNull() ?: return
        genreSeed = newGenreSeed
    }

    private fun List<PlayTrack>.copy(): List<PlayTrack> {
        return ArrayList(this.map { it.copy() })
    }

    companion object {
        private const val TAG = "MainViewModel"
    }

}
