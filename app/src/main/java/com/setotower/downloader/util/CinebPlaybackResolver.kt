package com.setotower.downloader.util

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Resolves Cineb's provider page to the short-lived media URL used by the
 * native player. The WebView is never shown to the user and is destroyed as
 * soon as the native player has a usable URL.
 */
class CinebPlaybackResolver(
    context: Context,
    private val onResolved: (String) -> Unit,
    private val onFailure: (String) -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val resolved = AtomicBoolean(false)
    private val webView = WebView(context)
    private var timeout: Runnable? = null

    init {
        configure()
    }

    /** Keep the resolver attached so Chromium actually executes the provider page. */
    fun attachTo(parent: ViewGroup) {
        if (webView.parent == null) {
            parent.addView(webView, ViewGroup.LayoutParams(1, 1))
            webView.alpha = 0.01f
            webView.isClickable = false
            webView.importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configure() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            cacheMode = WebSettings.LOAD_NO_CACHE
            userAgentString = WebSettings.getDefaultUserAgent(webView.context)
        }
        CookieManager.getInstance().setAcceptCookie(true)
        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                val url = request?.url?.toString().orEmpty()
                if (isMediaUrl(url)) resolveMediaUrl(url)
                return super.shouldInterceptRequest(view, request)
            }

            override fun shouldInterceptRequest(view: WebView?, url: String?): WebResourceResponse? {
                val candidate = url.orEmpty()
                if (isMediaUrl(candidate)) resolveMediaUrl(candidate)
                return super.shouldInterceptRequest(view, url)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                view?.evaluateJavascript(
                    "document.querySelectorAll('video').forEach(function(v){v.muted=true;v.play().catch(function(){});});",
                    null
                )
            }
        }
    }

    fun resolve(providerUrl: String) {
        resolved.set(false)
        timeout?.let(mainHandler::removeCallbacks)
        timeout = Runnable { fail("The movie source did not expose a playable stream.") }
            .also { mainHandler.postDelayed(it, 25_000L) }
        webView.loadUrl(providerUrl)
    }

    private fun resolveMediaUrl(url: String) {
        if (!resolved.compareAndSet(false, true)) return
        timeout?.let(mainHandler::removeCallbacks)
        mainHandler.post { onResolved(url) }
    }

    private fun fail(message: String) {
        if (!resolved.compareAndSet(false, true)) return
        onFailure(message)
    }

    private fun isMediaUrl(value: String): Boolean {
        val url = value.lowercase()
        return url.contains(".m3u8") || url.contains(".mpd") || url.contains(".mp4")
    }

    fun destroy() {
        timeout?.let(mainHandler::removeCallbacks)
        (webView.parent as? ViewGroup)?.removeView(webView)
        webView.stopLoading()
        webView.webViewClient = WebViewClient()
        webView.destroy()
    }
}
