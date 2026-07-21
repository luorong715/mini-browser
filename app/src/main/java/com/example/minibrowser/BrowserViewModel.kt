package com.example.minibrowser

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.minibrowser.data.HistoryRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first

// ⚠️ 改为继承 AndroidViewModel 以获取 Application Context
class BrowserViewModel(application: Application) : AndroidViewModel(application) {

    var uiState by mutableStateOf(BrowserState())
        private set

    val navigator = WebViewNavigatorImpl()

    // ⬇️ Step B 核心：持有 Repository 实例
    //private val historyRepository = HistoryRepository(application)
     val historyRepository = HistoryRepository(application)
    // ✅ 新增：初始化时从 DataStore 异步读取已保存的皮肤
    init {
        viewModelScope.launch {
            val savedSkin = SkinPreferences.getSkinFlow(application).first()
            uiState = uiState.copy(currentSkin = savedSkin)
        }
    }
    // ✅ 新增：切换皮肤并更新 UI 状态
    fun onSkinChanged(skin: SkinType) {
        uiState = uiState.copy(currentSkin = skin)
        viewModelScope.launch {
            SkinPreferences.saveSkin(getApplication(), skin)
        }
    }
    fun onUrlInputChanged(newInput: String) {
        uiState = uiState.copy(inputUrl = newInput)
    }

    fun onLoadRequested() {
        uiState = uiState.copy(currentUrl = uiState.inputUrl)
        navigator.loadUrl(uiState.inputUrl)
    }

    fun onLoadingChanged(isLoading: Boolean) {
        uiState = uiState.copy(isLoading = isLoading)
    }

    fun onNavigationChanged(canGoBack: Boolean, canGoForward: Boolean) {
        uiState = uiState.copy(canGoBack = canGoBack, canGoForward = canGoForward)
    }

    // ⬇️ Step B 核心：接收 WebView 传来的页面信息并异步写入数据库
    fun onPageLoaded(url: String, title: String) {
        viewModelScope.launch {
            historyRepository.addHistory(url, title)
        }
    }

    fun goBack() = navigator.goBack()
    fun goForward() = navigator.goForward()
    fun stopLoading() = navigator.stopLoading()
    fun reload() = navigator.reload()

    override fun onCleared() {
        super.onCleared()
        navigator.unbind()
    }
}
