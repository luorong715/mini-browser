package com.example.minibrowser

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.minibrowser.data.HistoryRepository
import kotlinx.coroutines.launch

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = HistoryRepository(application)

    // 使用 Flow 收集，UI 层自动响应数据库变化
    val historyList = repository.getHistoryFlow()

    var uiState by mutableStateOf(HistoryUiState())
        private set

    fun onDelete(id: Long) {
        viewModelScope.launch {
            repository.deleteHistory(id)
        }
    }

    fun onClearAll() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    fun onSelectUrl(url: String) {
        uiState = uiState.copy(selectedUrl = url)
    }

    fun resetSelection() {
        uiState = uiState.copy(selectedUrl = null)
    }
}

data class HistoryUiState(
    val selectedUrl: String? = null // 用于通知主界面跳转
)
