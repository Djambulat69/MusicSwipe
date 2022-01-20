package com.isaev.musicswipe

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.isaev.musicswipe.databinding.ActivityWebViewBinding
import kotlinx.coroutines.launch

class WebViewActivity : AppCompatActivity() {

    private val binding: ActivityWebViewBinding by lazy { ActivityWebViewBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

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
                                    AuthorizationManager.authorize(code)
                                } catch (e: Exception) {
                                    Log.i(TAG, e.stackTraceToString())
                                } finally {
                                    finish()
                                }
                            }
                        } else {
                            finish()
                        }
                        true
                    } else {
                        super.shouldOverrideUrlLoading(view, request)
                    }
                }
            }
            lifecycleScope.launch {
                val url = AuthorizationManager.authorizeUrl()
                loadUrl(url)
            }
        }
    }

    companion object {
        const val TAG = "WebViewActivity"

        fun startFrom(context: Context) {
            val intent = Intent().apply {
                setClass(context, WebViewActivity::class.java)
            }
            context.startActivity(intent)
        }
    }

}
