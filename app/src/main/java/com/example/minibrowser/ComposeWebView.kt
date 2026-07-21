package com.example.minibrowser

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun ComposeWebView(
    url: String,
    navigator: WebViewNavigator,
    modifier: Modifier = Modifier,
    onPageStarted: () -> Unit = {},
    // ⬇️ Step B 核心变更：签名改为携带 url 和 title
    onPageFinished: (url: String, title: String) -> Unit = { _, _ -> },
    onNavigationChanged: (canGoBack: Boolean, canGoForward: Boolean) -> Unit = { _, _ -> }
) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    // 兜底首次加载与状态恢复
    LaunchedEffect(url) {
        webViewRef?.let { wv ->
            if (wv.url != url) {
                wv.loadUrl(url)
            }
        }
    }

    // Navigator 绑定
    LaunchedEffect(webViewRef) {
        webViewRef?.let { wv ->
            (navigator as? WebViewNavigatorImpl)?.bind(wv)
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    cacheMode = WebSettings.LOAD_DEFAULT
                    setSupportZoom(true)
                    builtInZoomControls = true
                    displayZoomControls = false
                    useWideViewPort = true
                    loadWithOverviewMode = true
                }

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                        val targetUrl = request.url.toString()
                        if (targetUrl.startsWith("http://") || targetUrl.startsWith("https://")) {
                            return false
                        }
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl))
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            view.context.startActivity(intent)
                        } catch (_: Exception) {}
                        return true
                    }

                    override fun onPageStarted(view: WebView?, pageUrl: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, pageUrl, favicon)
                        onPageStarted()
                    }

                    // ⬇️ Step B 核心变更：提取标题并回传
                    override fun onPageFinished(view: WebView?, pageUrl: String?) {
                        super.onPageFinished(view, pageUrl)
                        val title = view?.title ?: ""
                        if (!pageUrl.isNullOrBlank()) {
                            onPageFinished(pageUrl, title)
                        }
                        view?.let { onNavigationChanged(it.canGoBack(), it.canGoForward()) }
                    }
                }

                webViewRef = this
            }
        },
        update = { _ -> },
        onRelease = { webView ->
            webView.stopLoading()
            webView.clearCache(true)
            webView.removeAllViews()
            webView.destroy()
            (navigator as? WebViewNavigatorImpl)?.unbind()
            webViewRef = null
        }
    )
}

/**
 * WebViewNavigator 内部实现（保持不变）
 */
class WebViewNavigatorImpl : WebViewNavigator {
    private var webView: WebView? = null

    internal fun bind(wv: WebView) { webView = wv }
    internal fun unbind() { webView = null }

    override fun loadUrl(url: String) { webView?.loadUrl(url) }
    override fun goBack() { webView?.goBack() }
    override fun goForward() { webView?.goForward() }
    override fun stopLoading() { webView?.stopLoading() }
    override fun reload() { webView?.reload() }
}
