package com.isaev.musicswipe

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.isaev.musicswipe.databinding.ActivityMainBinding
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse
import com.yuyakaido.android.cardstackview.CardStackLayoutManager
import com.yuyakaido.android.cardstackview.CardStackListener
import com.yuyakaido.android.cardstackview.Direction
import com.yuyakaido.android.cardstackview.StackFrom

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            authorize()
        }

        with(binding) {
            cardStack.layoutManager = CardStackLayoutManager(
                this@MainActivity, object : CardStackListener {
                    override fun onCardAppeared(view: View?, position: Int) {
                        if (savedInstanceState != null) { // Prevents preparing track again on rotation
                            val savedTopPosition: Int = savedInstanceState.getInt(CARD_STACK_STATE_KEY)
                            if (savedTopPosition == position) return
                        }

                        val adapter = cardStack.adapter as TracksAdapter

                        val cardsLeft = adapter.itemCount - position
                        if (cardsLeft < 5) {
                            viewModel.loadRecommendations()
                        }

                        val playTrack = adapter.tracks[position]

                        playTrack.track.previewUrl?.let {
                            viewModel.prepareNewTrack(playTrack.track.previewUrl, position)
                        }
                    }

                    override fun onCardCanceled() {}
                    override fun onCardDisappeared(view: View?, position: Int) {}
                    override fun onCardDragging(direction: Direction?, ratio: Float) {}
                    override fun onCardRewound() {}
                    override fun onCardSwiped(direction: Direction?) {}
                }
            ).apply {
                setStackFrom(StackFrom.Top)
                setTranslationInterval(8.0f)
            }
            cardStack.adapter = TracksAdapter(TracksDiffCallback()) { position ->
                viewModel.onTrackPlayClicked(position)
            }
        }

        viewModel.tracks.observe(this) { tracks ->
            (binding.cardStack.adapter as TracksAdapter).tracks = tracks
        }
    }

    override fun onPause() {
        super.onPause()
/*        TODO - make track pause in onPause

            viewModel.tracks.value?.let {
            val playingTrackIndex = it.indexOfFirst { track -> track.isPlaying }.takeIf { i -> i > -1 }
            playingTrackIndex?.let {
                viewModel.onTrackPlayClicked(playingTrackIndex)
            }
        }*/
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(CARD_STACK_STATE_KEY, (binding.cardStack.layoutManager as CardStackLayoutManager).topPosition)
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        val savedTopPosition: Int = savedInstanceState.getInt(CARD_STACK_STATE_KEY)
        (binding.cardStack.layoutManager as CardStackLayoutManager).topPosition = savedTopPosition
    }

    private fun authorize() {
        val request: AuthorizationRequest = AuthorizationRequest.Builder(
            CLIENT_ID, AuthorizationResponse.Type.TOKEN, "http://music.swipe.com"
        ).apply {
            setScopes(arrayOf("user-library-modify", "user-library-read"))
        }.build()

        val loginLauncher = getLoginLauncher()
        loginLauncher.launch(request)
    }

    private fun getLoginLauncher() = registerForActivityResult(SpotifyLoginActivityResultContract()) { response ->
        response?.let {
            when (response.type) {
                AuthorizationResponse.Type.TOKEN -> {
                    viewModel.token = response.accessToken
                }
                else -> {
                    Toast
                        .makeText(this, "Something went wrong. ${response.type.name}", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    private companion object {
        const val CLIENT_ID = "ff565d0979aa4da5810b5f3d55057c8f"
        const val CARD_STACK_STATE_KEY = "card_stack_state_key"
    }
}
