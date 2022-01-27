package com.isaev.musicswipe.ui

import android.media.MediaPlayer
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.isaev.musicswipe.PlaybackState
import com.isaev.musicswipe.data.SpotifyRepository
import com.isaev.musicswipe.data.Track
import com.isaev.musicswipe.data.User
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

class TracksViewModel @Inject constructor(
    private val mediaPlayer: MediaPlayer,
    private val spotifyRepository: SpotifyRepository
) : ViewModel() {

    private val _tracks = MutableLiveData<List<Track>>()
    private val _user = MutableLiveData<User>()
    private val _playbackState = MutableLiveData<PlaybackState>(
        PlaybackState(isPlaying = false, firstPlay = false)
    )
    private val _loading = MutableLiveData<Boolean>()

    private val _completeEvents: MutableSharedFlow<Unit> = MutableSharedFlow()
    private val _errorLikeEvents: MutableSharedFlow<Unit> = MutableSharedFlow()

    private var isLoadingRecommendations = false

    private var isPreparing = false

    private val trackSeeds: Queue<String> = ArrayDeque()
    private var artistSeed = ""
    private var genreSeed = ""

    val tracks: LiveData<List<Track>> = _tracks
    val user: LiveData<User> = _user
    val playbackState: LiveData<PlaybackState> = _playbackState
    val loading: LiveData<Boolean> = _loading
    val authState: Flow<Boolean> = spotifyRepository.authState

    val completeEvents: SharedFlow<Unit> = _completeEvents.asSharedFlow()
    val errorLikeEvents: SharedFlow<Unit> = _errorLikeEvents.asSharedFlow()

    val currentPosition: Int
        get() = mediaPlayer.currentPosition

    val duration: Int
        get() = mediaPlayer.duration

    init {
        authState
            .onEach { isAuthorized ->
                coroutineScope {
                    if (isAuthorized) {
                        _loading.value = true
                        launch {
                            getTopTracksSeeds()
                            getMoreRecommendations(10)
                            _loading.value = false
                        }
                        launch {
                            getMyUser()
                        }
                    }
                }
            }
            .catch { e -> Log.i(TAG, e.stackTraceToString()) }
            .launchIn(viewModelScope)

        mediaPlayer.setOnCompletionListener {
            viewModelScope.launch { _completeEvents.emit(Unit) }
        }
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
        if (mediaPlayer.isPlaying) {
            mediaPlayer.pause()
        }
    }

    fun loadRecommendations(limit: Int = 10) {
        if (isLoadingRecommendations) return
        isLoadingRecommendations = true
        viewModelScope.launch {
            try {
                getMoreRecommendations(limit)
            } catch (e: Exception) {
                Log.i(TAG, e.stackTraceToString())
            } finally {
                isLoadingRecommendations = false
            }
        }
    }

    fun prepareNewTrack(url: String) {
        isPreparing = true
        _playbackState.value = PlaybackState(isPlaying = false, false)
        with(mediaPlayer) {
            reset()
            setDataSource(url)
            prepareAsync()
            setOnPreparedListener {
                isPreparing = false
                onPlayClicked(true)
            }
        }
    }

    fun onPlayClicked(firstPlay: Boolean = false) {
        if (isPreparing || _tracks.value == null) return

        val isPlaying = playbackState.value?.isPlaying == true
        _playbackState.value = PlaybackState(!isPlaying, firstPlay)
    }

    fun onTrackLiked(track: Track) {
        trackSeeds.poll()
        trackSeeds.offer(track.id)
        val mainArtistId = track.artists.first().id
        artistSeed = mainArtistId
        viewModelScope.launch {
            try {
                val newGenreSeed: String =
                    spotifyRepository.getArtistTopGenre(mainArtistId) ?: return@launch
                genreSeed = newGenreSeed
                getMoreRecommendations(5)
            } catch (e: Exception) {
                _errorLikeEvents.emit(Unit)
                Log.i(TAG, "onTrackLiked: ${e.stackTraceToString()}")
            }
        }
    }

    private suspend fun getMoreRecommendations(limit: Int) {
        val recommendations = spotifyRepository.getRecommendations(
            arrayOf(artistSeed),
            arrayOf(genreSeed),
            trackSeeds.toTypedArray(),
            limit
        )

        _tracks.value = _tracks.value?.plus(recommendations) ?: recommendations
    }

    private suspend fun getTopTracksSeeds() {
        val topTracks = spotifyRepository.getTopTracks(3)

        trackSeeds.clear()
        trackSeeds.addAll(topTracks.map { it.id })

        val topMainArtistId = topTracks.first().artists.first().id

        artistSeed = topMainArtistId

        val newGenreSeed: String = spotifyRepository.getArtistTopGenre(topMainArtistId) ?: return
        genreSeed = newGenreSeed
    }

    private suspend fun getMyUser() {
        val user = spotifyRepository.getMe()
        _user.value = user
    }

    companion object {
        private const val TAG = "TracksViewModel"
    }

}
