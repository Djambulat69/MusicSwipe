package com.isaev.musicswipe

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.isaev.musicswipe.databinding.FragmentWebViewBinding
import kotlinx.coroutines.launch
import javax.inject.Inject

class WebViewFragment : Fragment(R.layout.fragment_web_view) {

    private var _binding: FragmentWebViewBinding? = null
    private val binding: FragmentWebViewBinding get() = _binding!!

    @Inject
    lateinit var authorizationManager: AuthorizationManager


    override fun onAttach(context: Context) {
        super.onAttach(context)
        MusicSwipeApp.instance.daggerComponent.inject(this)
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
                            lifecycleScope.launch {
                                try {
                                    authorizationManager.authorize(code)
                                } catch (e: Exception) {
                                    Log.i(TAG, e.stackTraceToString())
                                } finally {
                                    openTracks()
                                }
                            }
                        } else {
                            openLogin()
                        }
                        true
                    } else {
                        super.shouldOverrideUrlLoading(view, request)
                    }
                }
            }
            viewLifecycleScope.launch {
                val url = authorizationManager.authorizeUrl()
                loadUrl(url)
            }
        }
    }

    private fun openTracks() {
        fragmentInteractor?.openTracks()
    }

    private fun openLogin() {
        fragmentInteractor?.openLogin()
    }

    companion object {
        private const val TAG = "WebViewFragment"

        fun newInstance() = WebViewFragment()
    }

}
