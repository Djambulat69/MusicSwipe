package com.isaev.musicswipe

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.ArrayList

class TracksViewModel : ViewModel() {

    private var spotifyWebApiHelper: SpotifyWebApiHelper? = null

    private val _tracks = MutableLiveData<List<PlayTrack>>()
    private val _playbackState = MutableLiveData<PlaybackState>()

    private val _completeEvents: MutableSharedFlow<Unit> = MutableSharedFlow()

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
        setOnCompletionListener {
            viewModelScope.launch { _completeEvents.emit(Unit) }
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
    val playbackState: LiveData<PlaybackState> = _playbackState

    val completeEvents: SharedFlow<Unit> = _completeEvents.asSharedFlow()

    val currentPosition: Int
        get() = mediaPlayer.currentPosition

    val duration: Int
        get() = mediaPlayer.duration

    override fun onCleared() {
        mediaPlayer.release()
    }

    fun startPlayer() {
        mediaPlayer.start()
    }

    fun pausePlayer() {
        mediaPlayer.pause()
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
                ) ?: return@launch

                val newTracks =
                    recommendationsResponse.tracks.filter { it.previewUrl != null }.map { PlayTrack(it) }

                _tracks.value = _tracks.value?.plus(newTracks) ?: newTracks
            } catch (e: Exception) {
                Log.i(TAG, e.stackTraceToString())
            } finally {
                isLoadingRecommendations = false
            }
        }
    }


    fun prepareNewTrack(url: String, position: Int) {
        isPreparing = true
        _playbackState.value = PlaybackState(isPlaying = false, false)
        with(mediaPlayer) {
            reset()
            setDataSource(url)
            prepareAsync()
            setOnPreparedListener { mp ->
                isPreparing = false
                onTrackPlayClicked(position, true)
            }
        }
    }

    fun onTrackPlayClicked(position: Int, firstPlay: Boolean = false) {
        if (isPreparing) return

        val currentTracks = _tracks.value

        if (currentTracks != null) {
            onTrackPlayClicked(currentTracks[position], firstPlay)
        }
    }

    fun onTrackPlayClicked(playTrack: PlayTrack, firstPlay: Boolean = false) {
        if (isPreparing) return

        val isPlaying = playTrack.isPlaying

        playTrack.isPlaying = !isPlaying

        _playbackState.value = PlaybackState(isPlaying, firstPlay)
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
        private const val TAG = "TracksViewModel"
    }

}
