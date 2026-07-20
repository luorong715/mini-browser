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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    // ⚠️ Step 1 核心改动：通过参数注入 ViewModel
    viewModel: BrowserViewModel = viewModel()
) {
    // ⚠️ Step 1 核心改动：删除 remember/mutableStateOf，直接读取 ViewModel 状态
    val state = viewModel.uiState

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = false,
                    onClick = viewModel::goBack,
                    enabled = state.canGoBack,
                    icon = {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "后退")
                    }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = {
                        if (state.isLoading) viewModel.stopLoading()
                        else viewModel.reload()
                    },
                    icon = {
                        Icon(
                            imageVector = if (state.isLoading) Icons.Default.Close else Icons.Default.Refresh,
                            contentDescription = if (state.isLoading) "停止" else "刷新"
                        )
                    }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = viewModel::goForward,
                    enabled = state.canGoForward,
                    icon = {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "前进")
                    }
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
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = state.inputUrl,
                    //改为调用 ViewModel 方法，让ui层只负责展示状态和上报事件，让他无法修改状态
                    onValueChange = viewModel::onUrlInputChanged,
                    modifier = Modifier
                        .weight(1f)
                        .onKeyEvent { event ->
                            // 键盘回车触发加载
                            if (event.key == Key.Enter || event.key == Key.NumPadEnter) {
                                viewModel.onLoadRequested()
                                true
                            } else false
                        },
                    singleLine = true,
                    placeholder = { Text("输入网址") }
                )
                Button(onClick = viewModel::onLoadRequested) {
                    Text("Go")
                }
            }

            // 加载进度条
            if (state.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            // WebView 容器
          /*  ComposeWebView(
                url = state.currentUrl,
                modifier = Modifier.weight(1f),
                // ⚠️ 回调全部改为调用 ViewModel 方法
                onPageStarted = { viewModel.onLoadingChanged(true) },
                onPageFinished = { viewModel.onLoadingChanged(false) },
                onNavigationChanged = viewModel::onNavigationChanged,
                // webViewCreated 暂时保留原样，Step 2 再彻底解耦
                 // webViewCreated = { /* 暂留空 */ }
            ) */

            // 移除 webViewCreated，改为传递 navigator：
            ComposeWebView(
                url = state.currentUrl,
                navigator = viewModel.navigator, // ⬅️ 替换 webViewCreated
                modifier = Modifier.weight(1f),
                onPageStarted = { viewModel.onLoadingChanged(true) },
                onPageFinished = { viewModel.onLoadingChanged(false) },
                onNavigationChanged = viewModel::onNavigationChanged
            )

        }
    }
}
