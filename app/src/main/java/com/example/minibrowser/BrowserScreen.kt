package com.example.minibrowser

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    viewModel: BrowserViewModel = viewModel()
) {
    val state = viewModel.uiState

    // ⬇️ Step C 核心变更：实例化 HistoryViewModel 并监听选中状态
    val historyViewModel: HistoryViewModel = viewModel()
    val historyState = historyViewModel.uiState

    // ⬇️ Step C 核心变更：当历史记录被点击时，触发主浏览器跳转
    LaunchedEffect(historyState.selectedUrl) {
        historyState.selectedUrl?.let { url ->
            viewModel.onUrlInputChanged(url)
            viewModel.onLoadRequested()
            historyViewModel.resetSelection()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = false,
                    onClick = { viewModel.goBack() },
                    icon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "后退") },
                    enabled = state.canGoBack
                )
                NavigationBarItem(
                    selected = false,
                    onClick = {
                        if (state.isLoading) viewModel.stopLoading() else viewModel.reload()
                    },
                    icon = {
                        Icon(
                            if (state.isLoading) Icons.Filled.Close else Icons.Filled.Refresh,
                            contentDescription = if (state.isLoading) "停止" else "刷新"
                        )
                    }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { viewModel.goForward() },
                    icon = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "前进") },
                    enabled = state.canGoForward
                )
                // ⬇️ Step C 核心变更：新增历史记录入口 Tab
                NavigationBarItem(
                    selected = false,
                    onClick = { /* TODO: Step D 将实现真正的页面切换 */ },
                    icon = { Icon(Icons.Filled.History, contentDescription = "历史") }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 地址栏 + Go 按钮
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = state.inputUrl,
                    onValueChange = viewModel::onUrlInputChanged,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text("输入网址") }
                )
                Button(
                    onClick = viewModel::onLoadRequested,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text("Go")
                }
            }

            // 进度条
            if (state.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            // WebView 主体
            ComposeWebView(
                url = state.currentUrl,
                navigator = viewModel.navigator,
                modifier = Modifier.weight(1f),
                onPageStarted = { viewModel.onLoadingChanged(true) },
                onPageFinished = { url, title ->
                    viewModel.onLoadingChanged(false)
                    viewModel.onPageLoaded(url, title)
                },
                onNavigationChanged = viewModel::onNavigationChanged
            )

            // ⬇️ Step C 核心变更：临时渲染开关，用于验证 HistoryScreen 独立链路
            // 验收完成后请务必改回 false，避免与 Step D 的路由逻辑冲突
            if (false) {
                HistoryScreen(viewModel = historyViewModel)
            }
        }
    }
}
