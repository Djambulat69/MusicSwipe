package com.isaev.musicswipe

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.isaev.musicswipe.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), FragmentInteractor {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {

            lifecycleScope.launch {
                try {
                    AuthorizationManager.initTokens()
                } catch (e: Exception) {
                    Log.i(TAG, e.stackTraceToString())
                }
                if (AuthorizationManager.isAuthorized()) {
                    supportFragmentManager.commit {
                        add(R.id.fragment_container, TracksFragment.newInstance(), null)
                    }
                } else {
                    supportFragmentManager.commit {
                        add(R.id.fragment_container, LoginFragment.newInstance(), null)
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

    override fun openLogin() {
        replaceFragment(LoginFragment.newInstance())
    }

    override fun openLoginWebView() {
        replaceFragment(WebViewFragment.newInstance())
    }

    override fun openTracks() {
        replaceFragment(TracksFragment.newInstance())
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.commit {
            replace(R.id.fragment_container, fragment)
        }
    }

    private companion object {
        const val TAG = "MainActivity"
    }
}
