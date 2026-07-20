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
    onPageFinished: () -> Unit = {},
    onNavigationChanged: (canGoBack: Boolean, canGoForward: Boolean) -> Unit = { _, _ -> }
) {
    // 安全持有 WebView 引用，仅在组合树内有效
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    // ⚠️ 修复白屏：监听 url 参数变化，处理首次加载及外部状态同步
    LaunchedEffect(url) {
        webViewRef?.let { wv ->
            // 只有当 WebView 当前实际加载的 URL 与传入的 url 不一致时才加载
            // 这既保证了 App 启动时的首次加载，又避免了与 Navigator 的手动加载冲突
            if (wv.url != url) {
                wv.loadUrl(url)
            }
        }
    }

    // 绑定 Navigator 的逻辑保持不变，可以合并或单独写一个 LaunchedEffect
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

                    override fun onPageFinished(view: WebView?, pageUrl: String?) {
                        super.onPageFinished(view, pageUrl)
                        onPageFinished()
                        view?.let { onNavigationChanged(it.canGoBack(), it.canGoForward()) }
                    }
                }

                // 记录引用，触发 LaunchedEffect
                webViewRef = this
            }
        },
        update = { /* 不再处理 url 加载，全部交给 Navigator */ },
        onRelease = { webView ->
            webView.stopLoading()
            webView.clearCache(true)
            webView.removeAllViews()
            webView.destroy()
            webViewRef = null
        }
    )
}

/**
 * WebViewNavigator 的内部实现类
 * 持有 WebView 引用，生命周期与 ComposeWebView 绑定
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
