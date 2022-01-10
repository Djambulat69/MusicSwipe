package com.isaev.musicswipe

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.core.view.doOnLayout
import androidx.core.view.marginBottom
import androidx.core.view.marginTop
import androidx.core.view.updatePadding
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
            restoreViewState(savedInstanceState)
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

            likeButton.setOnClickListener {
                cardStack.swipeRight()
            }

            spotifyButton.doOnLayout {
                cardStack.updatePadding(
                    top = spotifyButton.height +
                            spotifyButton.marginTop +
                            spotifyButton.marginBottom +
                            cardStack.paddingTop
                )
            }
            dislikeButton.doOnLayout {
                cardStack.updatePadding(
                    bottom = dislikeButton.height +
                            dislikeButton.marginBottom +
                            dislikeButton.marginTop +
                            cardStack.paddingBottom
                )
            }

            dislikeButton.setOnClickListener {
                cardStack.swipeLeft()
            }
        }

        viewModel.tracks.observe(viewLifecycleOwner) { tracks ->
            (binding.cardStack.adapter as TracksAdapter).tracks = tracks
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.playbackUpdates.collect { playTrack ->
                    (binding.cardStack.getChildViewHolder(
                        (binding.cardStack.layoutManager as CardStackLayoutManager).topView
                    ) as TrackViewHolder).updatePlayback(playTrack)
                }
            }
        }

        val installedSpotify =
            try {
                requireContext().packageManager.getPackageInfo("com.spotify.music", 0)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }

        if (installedSpotify) {
            binding.spotifyButton.text = getString(R.string.open_in_spotify)
            binding.spotifyButton.setOnClickListener {
                val topTrack = getCurrentPlayTrack()?.track
                if (topTrack != null) {
                    val intent = Intent().apply {
                        action = Intent.ACTION_VIEW
                        data = Uri.parse(topTrack.uri)
                        putExtra(
                            Intent.EXTRA_REFERRER, Uri.parse("android-app://${requireContext().packageName}")
                        )
                    }
                    startActivity(intent)
                }
            }
        } else {
            binding.spotifyButton.text = getString(R.string.download_spotify)
        }
    }

    override fun onResume() {
        super.onResume()

        val topTrack = getCurrentPlayTrack()
        if (topTrack != null && !topTrack.isPlaying) {
            viewModel.onTrackPlayClicked(topTrack)
        }
    }

    override fun onPause() {
        super.onPause()

        val topTrack = getCurrentPlayTrack()
        if (topTrack != null && topTrack.isPlaying) {
            viewModel.onTrackPlayClicked(topTrack)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(CARD_STACK_STATE_KEY, (binding.cardStack.layoutManager as CardStackLayoutManager).topPosition)
    }

    private fun restoreViewState(savedInstanceState: Bundle) {
        val savedTopPosition: Int = savedInstanceState.getInt(CARD_STACK_STATE_KEY)
        (binding.cardStack.layoutManager as CardStackLayoutManager).topPosition = savedTopPosition
    }

    private fun authorize() {
        val request: AuthorizationRequest = AuthorizationManager.request()
        getLoginLauncher().launch(request)
    }

    private fun getLoginLauncher() = registerForActivityResult(SpotifyLoginActivityResultContract()) { response ->
        response?.let {
            when (response.type) {
                AuthorizationResponse.Type.TOKEN -> viewModel.token = response.accessToken
                else -> snackBar("Something went wrong. ${response.type.name}").show()
            }
        }
    }

    private fun getCurrentPlayTrack(): PlayTrack? {
        val position = (binding.cardStack.layoutManager as CardStackLayoutManager?)?.topPosition
        return position?.let {
            (binding.cardStack.adapter as TracksAdapter).tracks.getOrNull(position)
        }
    }

    private companion object {
        const val TAG = "TracksFragment"

        const val CARD_STACK_STATE_KEY = "card_stack_state_key"
        const val TRACKS_PREFETCH_DISTANCE = 5
    }
}
