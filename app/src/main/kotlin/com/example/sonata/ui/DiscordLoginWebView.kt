package com.example.sonata.ui

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun DiscordLoginWebView(
    onTokenExtracted: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                }
                
                webViewClient = object : WebViewClient() {
                    private var pollingRunnable: Runnable? = null

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        
                        // Stop any existing polling
                        pollingRunnable?.let { view?.removeCallbacks(it) }

                        if (url?.contains("discord.com/app") == true || url?.contains("discord.com/channels/@me") == true) {
                            val runnable = object : Runnable {
                                override fun run() {
                                    extractToken(view) { token ->
                                        if (token != null && token.isNotBlank() && token != "null") {
                                            view?.removeCallbacks(this)
                                            onTokenExtracted(token)
                                        } else {
                                            view?.postDelayed(this, 1500)
                                        }
                                    }
                                }
                            }
                            pollingRunnable = runnable
                            view?.post(runnable)
                        }
                    }

                    // Ensure polling stops if we navigate away
                    override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        pollingRunnable?.let { view?.removeCallbacks(it) }
                    }
                }
                loadUrl("https://discord.com/login")
            }
        },
        update = { }
    )
}

private fun extractToken(webView: WebView?, callback: (String?) -> Unit) {
    val script = """
        (function() {
            try {
                // Check webpack storage first
                var m = [];
                window.webpackChunkdiscord_app.push([[''], {}, e => {
                    for (let i in e.c) m.push(e.c[i]);
                }]);
                var token = m.find(m => m?.exports?.default?.getToken !== void 0)?.exports?.default?.getToken();
                
                // Fallback to iframe/localstorage context
                if (!token) {
                    var iframe = document.createElement('iframe');
                    document.head.appendChild(iframe);
                    token = iframe.contentWindow.localStorage.getItem('token')?.replace(/^"|"$/g, '');
                    iframe.remove();
                }
                return token || "";
            } catch(e) { return ""; }
        })();
    """.trimIndent()

    webView?.evaluateJavascript(script) { result ->
        val token = result?.replace("\"", "")
        if (token != null && token.isNotBlank() && token != "null") {
            // Clean up WebView data
            CookieManager.getInstance().removeAllCookies(null)
            webView.clearCache(true)
            webView.clearFormData()
            webView.clearHistory()
            callback(token)
        } else {
            callback(null)
        }
    }
}
