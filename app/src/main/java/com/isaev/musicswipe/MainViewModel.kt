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

class MainViewModel : ViewModel() {

    private var spotifyWebApiHelper: SpotifyWebApiHelper? = null

    private val _tracks = MutableLiveData<List<PlayTrack>>()

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

    private val trackSeeds: Queue<String> = ArrayDeque<String>(3).apply {
        addAll(arrayOf("5KmJMi7MiBJtCeJ08lmKzo", "1RUubW9fHtIYwjl588PrhZ", "6xATKzdUO89mZvStODhUSR"))
    }
    private var artistSeed = "2RdwBSPQiwcmiDo9kixcl8"
    private var genreSeed = "pop"

    private val _genre = MutableLiveData(genreSeed)
    val genre: LiveData<String> = _genre

    var token: String? = null
        set(newToken) {
            field = newToken
            newToken?.let {
                spotifyWebApiHelper = SpotifyWebApiHelper(newToken)
            }
            loadRecommendations()
        }

    val tracks: LiveData<List<PlayTrack>> = _tracks


    override fun onCleared() {
        mediaPlayer.release()
    }

    fun loadRecommendations(limit: Int = 10) {
        if (isLoadingRecommendations) return
        isLoadingRecommendations = true
        viewModelScope.launch {
            try {
                val response = spotifyWebApiHelper?.getRecommendations(
                    arrayOf(artistSeed),
                    arrayOf(genreSeed),
                    trackSeeds.toTypedArray(),
                    limit
                )

                response?.let {
                    val newTracks = response.tracks.filter { it.previewUrl != null }.map { PlayTrack(it) }

                    _tracks.value = _tracks.value?.plus(newTracks) ?: newTracks
                    val names = response.tracks.map { it.name }
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
            val isPlaying = currentTracks[position].isPlaying
            if (isPlaying) mediaPlayer.pause() else mediaPlayer.start()

            val copy = currentTracks.copy()
            copy[position].isPlaying = !isPlaying

            _tracks.value = copy
        }
    }

    fun onTrackLiked(playTrack: PlayTrack) {
        trackSeeds.poll()
        trackSeeds.offer(playTrack.track.id)
        val mainArtistId = playTrack.track.artists.first().id
        artistSeed = mainArtistId
        viewModelScope.launch {
            val newGenreSeed: String? = spotifyWebApiHelper?.getArtist(mainArtistId)?.genres?.firstOrNull()
            newGenreSeed?.let {
                genreSeed = newGenreSeed
                _genre.value = newGenreSeed
            }
            loadRecommendations(5)
        }
    }

    private fun List<PlayTrack>.copy(): List<PlayTrack> {
        return ArrayList(this.map { it.copy() })
    }

    companion object {
        private const val TAG = "MainViewModel"
    }

}
