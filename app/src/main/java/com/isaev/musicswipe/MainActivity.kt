package com.isaev.musicswipe

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import com.isaev.musicswipe.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {

            MusicSwipeApp.instance.applicationScope.launch {
                AuthorizationManager.refreshTokens()
                if (!AuthorizationManager.isAuthorized()) {
                    authorize()
                }
            }

            supportFragmentManager.commit {
                add(R.id.fragment_container, TracksFragment())
            }
        }
    }

    private fun authorize() {
        WebViewActivity.startFrom(this)
    }

    private companion object {
        const val TAG = "MainActivity"
    }
}
