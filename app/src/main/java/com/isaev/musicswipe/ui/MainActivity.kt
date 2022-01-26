package com.isaev.musicswipe.ui

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.isaev.musicswipe.R
import com.isaev.musicswipe.data.SpotifyAuthService
import com.isaev.musicswipe.databinding.ActivityMainBinding
import com.isaev.musicswipe.myApplication
import kotlinx.coroutines.launch
import javax.inject.Inject

class MainActivity : AppCompatActivity(), FragmentInteractor {

    private lateinit var binding: ActivityMainBinding

    @Inject
    lateinit var spotifyAuthService: SpotifyAuthService

    override fun onCreate(savedInstanceState: Bundle?) {
        myApplication.daggerComponent.inject(this)
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            lifecycleScope.launch {
                try {
                    spotifyAuthService.refreshTokens()
                } catch (e: Exception) {
                    Log.i(TAG, e.stackTraceToString())
                } finally {
                    supportFragmentManager.commit {
                        add(R.id.fragment_container, TracksFragment.newInstance(), null)
                    }
                }
            }
        }
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
        } else {
            super.onBackPressed()
        }
    }

    override fun back() {
        supportFragmentManager.popBackStack()
    }

    override fun openLoginWebView() {
        openFragment(WebViewFragment.newInstance())
    }

    override fun openTracks() {
        replaceFragment(TracksFragment.newInstance())
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.commit {
            replace(R.id.fragment_container, fragment)
        }
    }

    private fun openFragment(fragment: Fragment) {
        supportFragmentManager.commit {
            addToBackStack(null)
            replace(R.id.fragment_container, fragment)
        }
    }

    private companion object {
        const val TAG = "MainActivity"
    }
}
