package com.isaev.musicswipe

import android.content.ActivityNotFoundException
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
                requireContext(), TracksCardStackListener(viewModel, cardStack) { savedInstanceState }
            ).apply {
                setStackFrom(StackFrom.Top)
                setTranslationInterval(8f)
                setMaxDegree(60f)
            }

            cardStack.adapter =
                TracksAdapter(TracksDiffCallback(), cardStack.layoutManager as CardStackLayoutManager) { position ->
                    viewModel.onTrackPlayClicked(position)
                }

            likeButton.setOnClickListener {
                cardStack.swipeRight()
            }
            dislikeButton.setOnClickListener {
                cardStack.swipeLeft()
            }

            spotifyButton.doOnLayout {
                cardStack.updatePadding(
                    top = spotifyButton.height + spotifyButton.marginTop + spotifyButton.marginBottom +
                            cardStack.paddingTop
                )
            }
            dislikeButton.doOnLayout {
                cardStack.updatePadding(
                    bottom = dislikeButton.height + dislikeButton.marginBottom + dislikeButton.marginTop +
                            cardStack.paddingBottom
                )
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

        if (!installedSpotify) {
            binding.spotifyButton.text = getString(R.string.open_in_spotify)
            binding.spotifyButton.setOnClickListener {
                getCurrentPlayTrack()?.track?.let { topTrack ->
                    openTrackInSpotifyApp(topTrack.uri)
                }
            }
        } else {
            binding.spotifyButton.text = getString(R.string.download_spotify)
            binding.spotifyButton.setOnClickListener {
                openSpotifyInGooglePlayStore()
            }
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

    private fun openSpotifyInGooglePlayStore() {
        val appPackageName = "com.spotify.music"
        val referrer =
            "adjust_campaign=${requireContext().packageName}&adjust_tracker=ndjczk&utm_source=adjust_preinstall"

        try {
            val uri = Uri.parse("market://details")
                .buildUpon()
                .appendQueryParameter("id", appPackageName)
                .appendQueryParameter("referrer", referrer)
                .build()
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (_: ActivityNotFoundException) {
            val uri = Uri.parse("https://play.google.com/store/apps/details")
                .buildUpon()
                .appendQueryParameter("id", appPackageName)
                .appendQueryParameter("referrer", referrer)
                .build()
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
    }

    private fun openTrackInSpotifyApp(trackUri: String) {
        val intent = Intent().apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse(trackUri)
            putExtra(
                Intent.EXTRA_REFERRER, Uri.parse("android-app://${requireContext().packageName}")
            )
        }
        startActivity(intent)
    }

    companion object {
        const val TAG = "TracksFragment"

        const val CARD_STACK_STATE_KEY = "card_stack_state_key"
    }
}
