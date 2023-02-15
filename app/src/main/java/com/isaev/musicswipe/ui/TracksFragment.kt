package com.isaev.musicswipe.ui

import android.animation.ObjectAnimator
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.isaev.musicswipe.*
import com.isaev.musicswipe.data.Track
import com.isaev.musicswipe.databinding.FragmentTracksBinding
import com.yuyakaido.android.cardstackview.CardStackLayoutManager
import com.yuyakaido.android.cardstackview.StackFrom
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import java.net.URI
import javax.inject.Inject
import javax.inject.Provider

class TracksFragment : Fragment(R.layout.fragment_tracks) {

    private var _binding: FragmentTracksBinding? = null
    private val binding: FragmentTracksBinding get() = _binding!!

    @Inject
    lateinit var viewModelProvider: Provider<TracksViewModel>

    private val viewModel: TracksViewModel by viewModelsFactory { viewModelProvider.get() }

    private var progressAnimator: ObjectAnimator? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)

        myApplication.daggerComponent.inject(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentTracksBinding.bind(view)

        binding.cardStack.layoutManager = CardStackLayoutManager(
            requireContext(), TracksCardStackListener(
                viewModel,
                binding.cardStack,
                onPrepareTrack = {
                    binding.trackProgressCircle.isIndeterminate = true
                }
            ) { savedInstanceState }
        ).apply {
            setStackFrom(StackFrom.Top)
            setTranslationInterval(8f)
            setMaxDegree(60f)
        }

        with(binding) {
            cardStack.apply {
                setHasFixedSize(true)
                setItemViewCacheSize(5)
                isNestedScrollingEnabled = false
            }

            cardStack.adapter =
                TracksAdapter(TracksDiffCallback(), cardStack.layoutManager as CardStackLayoutManager)
            likeButton.setOnClickListener {
                cardStack.swipeRight()
            }
            dislikeButton.setOnClickListener {
                cardStack.swipeLeft()
            }
            playbackButton.setOnClickListener {
                getCurrentPlayTrack()?.let {
                    viewModel.onPlayClicked()
                }
            }

            loginButton.setOnClickListener {
//                fragmentInteractor?.openLoginWebView()


                val authRequest = viewModel.getAuthRequest()
                val authService = AuthorizationService(requireContext())
                val authIntent = authService.getAuthorizationRequestIntent(authRequest)

                startActivityForResult(authIntent, 0)
            }

            tracksToolbar.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.profile_menu_item -> {

                        if (viewModel.user.value == null) return@setOnMenuItemClickListener true

                        LogOutDialogFragment.newInstance(viewModel.user.value?.images?.lastOrNull()?.url)
                            .show(parentFragmentManager, null)
                        true
                    }
                    else -> false
                }
            }
        }

        progressAnimator = ObjectAnimator.ofInt(
            binding.trackProgressCircle, "progress",
            requireContext().resources.getInteger(R.integer.trackProgressMax)
        ).apply {
            setAutoCancel(true)
            repeatCount = 0
            interpolator = LinearInterpolator()
        }

        viewModel.authState
            .flowWithLifecycle(viewLifecycleOwner.lifecycle, Lifecycle.State.CREATED)
            .map { it is Token.AccessToken }
            .onEach { isAuthorized ->
                with(binding) {
                    loginButton.isGone = isAuthorized
                    showPlayerUi(isAuthorized && viewModel.loading.value == false)
                }
            }
            .launchIn(viewLifecycleScope)

        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            with(binding) {
                showPlayerUi(!isLoading && viewModel.authState.value is Token.AccessToken)
                loadingCircle.isVisible = isLoading
            }
        }

        viewModel.tracks.observe(viewLifecycleOwner) { tracks ->
            (binding.cardStack.adapter as TracksAdapter).tracks = tracks
        }

        viewModel.playbackState.observe(viewLifecycleOwner) { playbackState ->
            if (!playbackState.isPlaying) {
                progressAnimator?.pause()
                viewModel.pausePlayer()
                binding.playbackButton.setImageResource(R.drawable.ic_baseline_play_arrow)
            } else {
                binding.playbackButton.setImageResource(R.drawable.ic_baseline_pause)

                viewModel.startPlayer()
                if (playbackState.firstPlay) {
                    beginProgressAnimationFromStart()
                } else {
                    if (progressAnimator?.isPaused == true) {
                        progressAnimator?.resume()
                    } else {
                        progressAnimator?.start()
                    }
                }
            }
        }

        viewModel.user.observe(viewLifecycleOwner) { user ->
            val profileMenuItem = binding.tracksToolbar.menu.findItem(R.id.profile_menu_item)
            if (user == null) {
                profileMenuItem.setIcon(R.drawable.ic_account_circle)
            } else {
                Glide.with(this)
                    .asDrawable()
                    .load(user.images.lastOrNull()?.url)
                    .circleCrop()
                    .into(
                        object : CustomTarget<Drawable>() {
                            override fun onLoadCleared(placeholder: Drawable?) {}

                            override fun onResourceReady(
                                profileIconDrawable: Drawable,
                                transition: Transition<in Drawable>?
                            ) {
                                profileMenuItem.icon = profileIconDrawable
                            }
                        }
                    )
            }
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
                getCurrentPlayTrack()?.let { topTrack ->
                    openTrackInSpotifyApp(topTrack.uri)
                }
            }
        } else {
            binding.spotifyButton.text = getString(R.string.get_spotify_free)
            binding.spotifyButton.setOnClickListener {
                openSpotifyInGooglePlayStore()
            }
        }

        if (savedInstanceState != null) {
            restoreViewState(savedInstanceState)
        }
    }

    override fun onResume() {
        super.onResume()

        val topTrack = getCurrentPlayTrack()
        if (topTrack != null && viewModel.playbackState.value?.isPlaying == false) {
            viewModel.onPlayClicked()
        }
    }

    override fun onPause() {
        super.onPause()

        val topTrack = getCurrentPlayTrack()
        if (topTrack != null && viewModel.playbackState.value?.isPlaying == true) {
            viewModel.onPlayClicked()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        progressAnimator?.cancel()
        progressAnimator = null
        _binding = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(CARD_STACK_STATE_KEY, (binding.cardStack.layoutManager as CardStackLayoutManager).topPosition)
    }

    private fun restoreViewState(savedInstanceState: Bundle) {
        val savedTopPosition: Int = savedInstanceState.getInt(CARD_STACK_STATE_KEY)
        (binding.cardStack.layoutManager as CardStackLayoutManager).topPosition = savedTopPosition

        progressAnimator?.duration = (viewModel.duration - viewModel.currentPosition).toLong()
    }

    private fun getCurrentPlayTrack(): Track? {
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
        progressAnimator?.setIntValues(0, requireContext().resources.getInteger(R.integer.trackProgressMax))
        progressAnimator?.duration = viewModel.duration.toLong()
        progressAnimator?.start()
    }

    private fun showPlayerUi(show: Boolean) {
        with(binding) {
            cardStack.isVisible = show
            playbackButton.isVisible = show
            trackProgressCircle.isVisible = show
            spotifyButton.isVisible = show
            dislikeButton.isVisible = show
            likeButton.isVisible = show
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (data == null) return

        if (requestCode == 0) {
            val resp = AuthorizationResponse.fromIntent(data)
            val ex = AuthorizationException.fromIntent(data)

            val authCode = resp?.authorizationCode
            val codeVerifier = resp?.request?.codeVerifier

            if (authCode != null && codeVerifier != null) {
                lifecycleScope.launch {
                    viewModel.authorize(authCode, codeVerifier)
                }
            }

            Log.i(TAG, "token=${resp?.accessToken.orEmpty()}", ex)
        }
    }

    companion object {
        fun newInstance() = TracksFragment()

        const val TAG = "TracksFragment"

        const val SPOTIFY_APP_PACKAGE_NAME = "com.spotify.music"
        const val CARD_STACK_STATE_KEY = "card_stack_state_key"
    }
}
