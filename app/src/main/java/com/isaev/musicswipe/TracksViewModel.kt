package com.isaev.musicswipe

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

class TracksViewModel : ViewModel() {

    private val spotifyWebApiHelper: SpotifyWebApiHelper = SpotifyWebApiHelper

    private val _tracks = MutableLiveData<List<PlayTrack>>()
    private val _playbackState = MutableLiveData<PlaybackState>()

    private val _completeEvents: MutableSharedFlow<Unit> = MutableSharedFlow()
    private val _errorLikeEvents: MutableSharedFlow<Unit> = MutableSharedFlow()

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

    private val trackSeeds: Queue<String> = ArrayDeque()
    private var artistSeed = ""
    private var genreSeed = ""

    val tracks: LiveData<List<PlayTrack>> = _tracks
    val playbackState: LiveData<PlaybackState> = _playbackState

    val completeEvents: SharedFlow<Unit> = _completeEvents.asSharedFlow()
    val errorLikeEvents: SharedFlow<Unit> = _errorLikeEvents.asSharedFlow()

    val currentPosition: Int
        get() = mediaPlayer.currentPosition

    val duration: Int
        get() = mediaPlayer.duration

    init {
        AuthorizationManager.authState
            .onEach { isAuthorized ->
                if (isAuthorized) {
                    viewModelScope.launch {
                        loadTopTracksSeeds()
                        loadRecommendations()
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    override fun onCleared() {
        mediaPlayer.release()
    }

    fun startPlayer() {
        if (!mediaPlayer.isPlaying) {
            mediaPlayer.start()
        }
    }

    fun pausePlayer() {
        mediaPlayer.pause()
    }

    fun loadRecommendations(limit: Int = 10) {
        if (isLoadingRecommendations) return
        isLoadingRecommendations = true
        viewModelScope.launch {
            try {
                val recommendationsResponse = spotifyWebApiHelper.getRecommendations(
                    arrayOf(artistSeed),
                    arrayOf(genreSeed),
                    trackSeeds.toTypedArray(),
                    limit
                )

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
            setOnPreparedListener {
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
                    spotifyWebApiHelper.getArtist(mainArtistId).genres.firstOrNull() ?: return@launch
                genreSeed = newGenreSeed
                loadRecommendations(5)
            } catch (e: Exception) {
                _errorLikeEvents.emit(Unit)
                Log.i(TAG, "onTrackLiked: ${e.stackTraceToString()}")
            }
        }
    }

    fun authorize(newToken: String) {
        AuthorizationManager.setToken(newToken)
    }

    private suspend fun loadTopTracksSeeds() {
        val topTracksResponse = spotifyWebApiHelper.getTopTracks(3)
        val topTracks = topTracksResponse.items

        trackSeeds.clear()
        trackSeeds.addAll(topTracks.map { it.id })

        val topMainArtistId = topTracks.first().artists.first().id

        artistSeed = topMainArtistId

        val newGenreSeed: String = spotifyWebApiHelper.getArtist(topMainArtistId).genres.firstOrNull() ?: return
        genreSeed = newGenreSeed
    }

    companion object {
        private const val TAG = "TracksViewModel"
    }

}
