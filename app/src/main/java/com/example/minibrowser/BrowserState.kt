package com.example.minibrowser

data class BrowserState(
    val currentUrl: String = "https://www.baidu.com",
    val inputUrl: String = "https://www.baidu.com",
    val isLoading: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false
)
