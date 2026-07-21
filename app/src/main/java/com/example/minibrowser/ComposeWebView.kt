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
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.minibrowser.data.HistoryEntity
import com.example.minibrowser.jsapi.MiniBrowserJsApi

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

                // ✅ JSAPI 注册
                addJavascriptInterface(
                    MiniBrowserJsApi(historyProvider),
                    "MiniBrowser"
                )

                // ✅ WebChromeClient：桥接 console.log + 监听加载进度
                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                        consoleMessage?.let {
                            android.util.Log.d("JS_Console", "[${it.messageLevel()}] ${it.message()}")
                        }
                        return true
                    }

                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        super.onProgressChanged(view, newProgress)
                        if (newProgress == 30) {
                            view?.evaluateJavascript(
                                "console.log('[Inject-Step1] onProgressChanged 30%: DOM正在构建，可注入CSS/全局变量');",
                                null
                            )
                        }
                    }
                }

                webViewClient = object : WebViewClient() {
                    // ✅ 增强版第三方 App 跳转支持
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                        val uri = request.url
                        val scheme = uri.scheme ?: return false

                        // 1. HTTP/HTTPS 正常交给 WebView 加载
                        if (scheme.equals("http", true) || scheme.equals("https", true)) {
                            return false
                        }

                        // 2. 非 HTTP 协议（第三方 App Scheme / mailto / tel 等）交由系统处理
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            }

                            // 检查是否有应用能处理该 Scheme，避免 ActivityNotFoundException
                            if (intent.resolveActivity(view.context.packageManager) != null) {
                                view.context.startActivity(intent)
                            } else {
                                // 未安装对应 App 时的降级提示
                                Toast.makeText(
                                    view.context,
                                    "未检测到相关应用，请安装后重试",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("WebView_Scheme", "第三方App跳转失败: $uri", e)
                            Toast.makeText(
                                view.context,
                                "无法打开该链接: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        // 3. 阻止 WebView 继续加载该非 HTTP URL
                        return true
                    }

                    override fun onPageStarted(view: WebView?, pageUrl: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, pageUrl, favicon)
                        onPageStarted()

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
                        )*/

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
 * WebViewNavigator 内部实现
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
