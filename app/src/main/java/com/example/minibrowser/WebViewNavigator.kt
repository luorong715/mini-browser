package com.example.minibrowser

/**
 * WebView 控制契约接口
 * ViewModel 通过此接口操控 WebView，无需持有原生引用
 */
interface WebViewNavigator {
    fun loadUrl(url: String)
    fun goBack()
    fun goForward()
    fun stopLoading()
    fun reload()
}
