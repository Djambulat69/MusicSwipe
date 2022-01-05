package com.isaev.musicswipe

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.isaev.musicswipe.databinding.FragmentTracksBinding
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse
import com.yuyakaido.android.cardstackview.CardStackLayoutManager
import com.yuyakaido.android.cardstackview.CardStackListener
import com.yuyakaido.android.cardstackview.Direction
import com.yuyakaido.android.cardstackview.StackFrom
import kotlinx.coroutines.launch

class TracksFragment : Fragment(R.layout.fragment_tracks) {

    private var _binding: FragmentTracksBinding? = null
    private val binding: FragmentTracksBinding get() = _binding!!

    private val viewModel: MainViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentTracksBinding.bind(view)

        if (savedInstanceState == null) {
            authorize()
        } else {
            onRestoreInstanceState(savedInstanceState)
        }

        with(binding) {
            cardStack.layoutManager = CardStackLayoutManager(
                requireContext(), object : CardStackListener {
                    override fun onCardAppeared(view: View?, position: Int) {
                        if (savedInstanceState != null) { // Prevents preparing track again on rotation
                            val savedTopPosition: Int = savedInstanceState.getInt(CARD_STACK_STATE_KEY)
                            if (savedTopPosition == position || position == 0) return
                        }

                        val adapter = cardStack.adapter as TracksAdapter

                        val cardsLeft = adapter.itemCount - position
                        if (cardsLeft < TRACKS_PREFETCH_DISTANCE) {
                            viewModel.loadRecommendations()
                        }

                        val playTrack = adapter.tracks.getOrNull(position)

                        playTrack?.track?.previewUrl?.let {
                            viewModel.prepareNewTrack(playTrack.track.previewUrl, position)
                        }
                    }

                    override fun onCardCanceled() {}
                    override fun onCardDisappeared(view: View?, position: Int) {}
                    override fun onCardDragging(direction: Direction?, ratio: Float) {}
                    override fun onCardRewound() {}
                    override fun onCardSwiped(direction: Direction?) {
                        if (direction == Direction.Right) {
                            val position = (cardStack.layoutManager as CardStackLayoutManager).topPosition - 1
                            val adapter = cardStack.adapter as TracksAdapter
                            val likedTrack = adapter.tracks.getOrNull(position)
                            likedTrack?.let {
                                viewModel.onTrackLiked(likedTrack)
                            }
                        }
                    }
                }
            ).apply {
                setStackFrom(StackFrom.Top)
                setTranslationInterval(8.0f)
            }

            cardStack.adapter =
                TracksAdapter(TracksDiffCallback(), cardStack.layoutManager as CardStackLayoutManager) { position ->
                    viewModel.onTrackPlayClicked(position)
                }
        }

        viewModel.tracks.observe(viewLifecycleOwner) { tracks ->
            (binding.cardStack.adapter as TracksAdapter).tracks = tracks
        }

        viewModel.genre.observe(viewLifecycleOwner) { genre ->
            binding.genreButton.text = getString(R.string.genre_placeholder, genre)
        }


        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.playbackUpdates.collect { playTrack ->
                    (binding.cardStack.getChildViewHolder(
                        (binding.cardStack.layoutManager as CardStackLayoutManager).topView
                    ) as TrackViewHolder).updatePlayback(playTrack)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(CARD_STACK_STATE_KEY, (binding.cardStack.layoutManager as CardStackLayoutManager).topPosition)
        super.onSaveInstanceState(outState)
    }

    private fun onRestoreInstanceState(savedInstanceState: Bundle) {
        val savedTopPosition: Int = savedInstanceState.getInt(CARD_STACK_STATE_KEY)
        (binding.cardStack.layoutManager as CardStackLayoutManager).topPosition = savedTopPosition
    }

    private fun authorize() {
        val request: AuthorizationRequest = AuthorizationRequest.Builder(
            CLIENT_ID, AuthorizationResponse.Type.TOKEN, REDIRECT_URI
        ).apply {
            setScopes(arrayOf("user-library-modify", "user-library-read", "user-top-read"))
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
                        .makeText(requireContext(), "Something went wrong. ${response.type.name}", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    private companion object {
        const val TAG = "TracksFragment"

        const val CLIENT_ID = "ff565d0979aa4da5810b5f3d55057c8f"
        const val REDIRECT_URI = "http://music.swipe.com"
        const val CARD_STACK_STATE_KEY = "card_stack_state_key"
        const val TRACKS_PREFETCH_DISTANCE = 5
    }
}
