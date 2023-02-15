package com.isaev.musicswipe.ui

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import com.isaev.musicswipe.R
import com.isaev.musicswipe.data.SpotifyAuthService
import com.isaev.musicswipe.databinding.FragmentWebViewBinding
import com.isaev.musicswipe.fragmentInteractor
import com.isaev.musicswipe.myApplication
import com.isaev.musicswipe.viewLifecycleScope
import kotlinx.coroutines.launch
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationServiceConfiguration
import javax.inject.Inject

class WebViewFragment : Fragment(R.layout.fragment_web_view) {

    private var _binding: FragmentWebViewBinding? = null
    private val binding: FragmentWebViewBinding get() = _binding!!

    @Inject
    lateinit var spotifyAuthService: SpotifyAuthService


    override fun onAttach(context: Context) {
        super.onAttach(context)
        myApplication.daggerComponent.inject(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentWebViewBinding.bind(view)

        with(binding.webView) {
            settings.javaScriptEnabled = true
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val url = request?.url
                    return if (url?.host == "music.swipe.com") {
                        val code = url.getQueryParameter("code")
                        if (code != null) {
                            myApplication.applicationScope.launch {
                                try {
//                                    spotifyAuthService.authorize(code)
                                } catch (e: Exception) {
                                    Log.i(TAG, e.stackTraceToString())
                                }
                            }
                        }
                        fragmentInteractor?.back()
                        true
                    } else {
                        super.shouldOverrideUrlLoading(view, request)
                    }
                }
            }
            viewLifecycleScope.launch {
                val url = spotifyAuthService.authorizeUrl()
                loadUrl(url)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "WebViewFragment"

        fun newInstance() = WebViewFragment()
    }

}
