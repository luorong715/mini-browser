package com.example.minibrowser

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
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
import com.example.minibrowser.jsapi.MiniBrowserJsApi
import com.example.minibrowser.data.HistoryEntity

@Composable
fun ComposeWebView(
    url: String,
    navigator: WebViewNavigator,
    historyProvider: () -> List<HistoryEntity> = { emptyList() },
    modifier: Modifier = Modifier,
    onPageStarted: () -> Unit = {},
    onPageFinished: (url: String, title: String) -> Unit = { _, _ -> },
    onNavigationChanged: (canGoBack: Boolean, canGoForward: Boolean) -> Unit = { _, _ -> }
) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    LaunchedEffect(url) {
        webViewRef?.let { wv ->
            if (wv.url != url) {
                wv.loadUrl(url)
            }
        }
    }

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
                // ✅ 在这里插入 JSAPI 注册（紧跟 settings 之后）
                addJavascriptInterface(
                    MiniBrowserJsApi(historyProvider),
                    "MiniBrowser"
                )

                // ✅ 补全 WebChromeClient：桥接 console.log + 监听加载进度
                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                        consoleMessage?.let {
                            android.util.Log.d("JS_Console", "[${it.messageLevel()}] ${it.message()}")
                        }
                        return true
                    }

                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        super.onProgressChanged(view, newProgress)
                        // 选择 30% 作为观察采样点，避免日志刷屏
                        if (newProgress == 30) {
                            view?.evaluateJavascript(
                                "console.log('[Inject-Step1] onProgressChanged 30%: DOM正在构建，可注入CSS/全局变量');",
                                null
                            )
                        }
                    }
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

                        // ✅ 1.4 第一步：注入观察 - 页面开始加载，DOM 尚未就绪
                        view?.evaluateJavascript(
                            "console.log('[Inject-Step1] onPageStarted: 此时DOM不可用，适合注入底层通信桥');",
                            null
                        )
                    }

                    override fun onPageFinished(view: WebView?, pageUrl: String?) {
                        super.onPageFinished(view, pageUrl)
                        val title = view?.title ?: ""
                        if (!pageUrl.isNullOrBlank()) {
                            onPageFinished(pageUrl, title)
                        }
                        view?.let { onNavigationChanged(it.canGoBack(), it.canGoForward()) }

                        // ✅ 1.4 第一步：注入观察 - 主文档加载完成，DOM 树完整可操作
                        // ⚠️ [临时调试代码 - 已验证通过]
                        // 用途：验证 window.MiniBrowser.getHistory() JSAPI 是否正常返回数据
                        // 状态：2026-07-21 验证成功，生产环境禁用
                        // 如需再次验证，取消下方注释即可
                        /*view?.evaluateJavascript(
                            """

                           (function() {
                                try {
                                    if (typeof window.MiniBrowser !== 'undefined') {
                                        var historyJson = window.MiniBrowser.getHistory();
                                        console.log('[Step2-Verify] JSAPI调用成功，返回数据: ' + historyJson);
                                    } else {
                                        console.log('[Step2-Verify] ❌ 失败: window.MiniBrowser 未定义');
                                    }
                                } catch (e) {
                                    console.log('[Step2-Verify] ❌ 异常: ' + e.message);
                                }
                            })();
                            document.title = '[端上注入验证] ' + document.title;
                            """.trimIndent(),
                            null
                        ) */
                        view?.evaluateJavascript(
                            "document.title = '[端上注入验证] ' + document.title;",
                            null
                        )
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
