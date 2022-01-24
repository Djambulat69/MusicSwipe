package com.isaev.musicswipe

import android.animation.ObjectAnimator
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.isaev.musicswipe.databinding.FragmentTracksBinding
import com.yuyakaido.android.cardstackview.CardStackLayoutManager
import com.yuyakaido.android.cardstackview.StackFrom
import kotlinx.coroutines.launch

class TracksFragment : Fragment(R.layout.fragment_tracks) {

    private var _binding: FragmentTracksBinding? = null
    private val binding: FragmentTracksBinding get() = _binding!!

    private val viewModel: TracksViewModel by viewModels()

    private val progressAnimator: ObjectAnimator by lazy {
        ObjectAnimator.ofInt(
            binding.trackProgressCircle, "progress",
            requireContext().resources.getInteger(R.integer.trackProgressMax)
        ).apply {
            setAutoCancel(true)
            repeatCount = 0
            interpolator = LinearInterpolator()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentTracksBinding.bind(view)

        binding.cardStack.layoutManager = CardStackLayoutManager(
            requireContext(), TracksCardStackListener(
                viewModel,
                binding.cardStack,
                onPrepareTrack = { binding.trackProgressCircle.isIndeterminate = true }
            ) { savedInstanceState }
        ).apply {
            setStackFrom(StackFrom.Top)
            setTranslationInterval(8f)
            setMaxDegree(60f)
        }

        if (savedInstanceState != null) {
            restoreViewState(savedInstanceState)
        }

        with(binding) {
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
            playbackButton.setOnClickListener {
                getCurrentPlayTrack()?.let { topTrack ->
                    viewModel.onTrackPlayClicked(topTrack)
                }
            }
        }

        viewModel.tracks.observe(viewLifecycleOwner) { tracks ->
            (binding.cardStack.adapter as TracksAdapter).tracks = tracks
        }

        viewModel.playbackState.observe(viewLifecycleOwner) { playbackState ->
            if (playbackState.isPlaying) {
                progressAnimator.pause()
                viewModel.pausePlayer()
                binding.playbackButton.setImageResource(R.drawable.ic_baseline_play_arrow)
            } else {
                binding.playbackButton.setImageResource(R.drawable.ic_baseline_pause)

                viewModel.startPlayer()
                if (playbackState.firstPlay) {
                    beginProgressAnimationFromStart()
                } else {
                    if (progressAnimator.isPaused) {
                        progressAnimator.resume()
                    } else {
                        progressAnimator.start()
                    }
                }
            }
        }

        viewModel.user.observe(viewLifecycleOwner) { user ->
            Glide.with(this)
                .asDrawable()
                .load(user.images.lastOrNull()?.url)
                .circleCrop()
                .into(
                    object : CustomTarget<Drawable>() {
                        override fun onLoadCleared(placeholder: Drawable?) {}

                        override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                            binding.tracksToolbar.menu.findItem(R.id.profile_menu_item).icon = resource
                        }
                    }
                )
        }

        viewLifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.completeEvents.collect {
                    viewModel.startPlayer()
                    beginProgressAnimationFromStart()
                }
            }
        }
        viewLifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.errorLikeEvents.collect {
                    makeSnackBar(getString(R.string.failed_like)).show()
                }
            }
        }

        val installedSpotify =
            try {
                requireContext().packageManager.getPackageInfo(SPOTIFY_APP_PACKAGE_NAME, 0)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }

        if (installedSpotify) {
            binding.spotifyButton.text = getString(R.string.open_spotify)
            binding.spotifyButton.setOnClickListener {
                getCurrentPlayTrack()?.track?.let { topTrack ->
                    openTrackInSpotifyApp(topTrack.uri)
                }
            }
        } else {
            binding.spotifyButton.text = getString(R.string.get_spotify_free)
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
        progressAnimator.cancel()
        _binding = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(CARD_STACK_STATE_KEY, (binding.cardStack.layoutManager as CardStackLayoutManager).topPosition)
    }

    private fun restoreViewState(savedInstanceState: Bundle) {
        val savedTopPosition: Int = savedInstanceState.getInt(CARD_STACK_STATE_KEY)
        (binding.cardStack.layoutManager as CardStackLayoutManager).topPosition = savedTopPosition
        progressAnimator.duration = (viewModel.duration - viewModel.currentPosition).toLong()
    }

    private fun getCurrentPlayTrack(): PlayTrack? {
        val position = (binding.cardStack.layoutManager as CardStackLayoutManager?)?.topPosition
        return position?.let {
            (binding.cardStack.adapter as TracksAdapter).tracks.getOrNull(position)
        }
    }

    private fun openSpotifyInGooglePlayStore() {
        val appPackageName = SPOTIFY_APP_PACKAGE_NAME
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

    private fun beginProgressAnimationFromStart() {
        binding.trackProgressCircle.setProgressCompat(0, true)
        progressAnimator.setIntValues(0, requireContext().resources.getInteger(R.integer.trackProgressMax))
        progressAnimator.duration = viewModel.duration.toLong()
        progressAnimator.start()
    }

    companion object {
        fun newInstance() = TracksFragment()

        const val TAG = "TracksFragment"

        const val SPOTIFY_APP_PACKAGE_NAME = "com.spotify.music"
        const val CARD_STACK_STATE_KEY = "card_stack_state_key"
    }
}
