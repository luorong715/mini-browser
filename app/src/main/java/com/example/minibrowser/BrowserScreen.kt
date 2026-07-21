package com.example.minibrowser

import android.content.res.Configuration
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    viewModel: BrowserViewModel = viewModel(), // ✅ Step D: 使用 hiltViewModel 绑定 NavBackStackEntry
    initialUrl: String? = null, // ✅ Step D: 接收路由参数
    onNavigateToHistory: () -> Unit = {} // ✅ Step D: 将页面切换交由外部 MainScreen 处理
) {
    val state = viewModel.uiState

    // ⚠️ Step C 的 historyViewModel、historyState、LaunchedEffect(selectedUrl) 已彻底移除
    // 两个页面的状态现在完全隔离，不再产生任何交叉污染

    // ✅ 换肤相关：获取系统深色模式状态，并生成对应 ColorScheme
    val isSystemDark = LocalConfiguration.current.uiMode and
            Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    val colorScheme = state.currentSkin.toColorScheme(isSystemDark)

    // ✅ Step D: 仅在首次接收到非空路由 URL 时触发加载
    LaunchedEffect(initialUrl) {
        if (!initialUrl.isNullOrEmpty()) {
            viewModel.onUrlInputChanged(initialUrl)
            viewModel.onLoadRequested()
        }
    }

    // ✅ 换肤相关：用动态 ColorScheme 包裹整个页面
    MaterialTheme(colorScheme = colorScheme) {
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
                    // ✅ Step D: 点击历史 Tab 时，仅发出导航意图，不直接操作 UI
                    NavigationBarItem(
                        selected = false,
                        onClick = onNavigateToHistory,
                        icon = { Icon(Icons.Filled.History, contentDescription = "历史") }
                    )
                    // ✅ 换肤相关：新增换肤切换按钮（循环切换4种皮肤）
                    NavigationBarItem(
                        selected = false,
                        onClick = {
                            val nextIndex = (state.currentSkin.ordinal + 1) % SkinType.entries.size
                            viewModel.onSkinChanged(SkinType.entries[nextIndex])
                        },
                        icon = { Text("🎨", style = MaterialTheme.typography.titleMedium) },
                        label = { Text(state.currentSkin.displayName, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
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

                if (state.isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                ComposeWebView(
                    url = state.currentUrl,
                    navigator = viewModel.navigator,
                    // ✅ 传入历史数据查询逻辑
                    historyProvider = {
                        runBlocking(Dispatchers.IO) {
                            viewModel.historyRepository
                                .getHistoryFlow()
                                .first()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    onPageStarted = { viewModel.onLoadingChanged(true) },
                    onPageFinished = { url, title ->
                        viewModel.onLoadingChanged(false)
                        viewModel.onPageLoaded(url, title)
                    },
                    onNavigationChanged = viewModel::onNavigationChanged
                )
            }
        }
    }
}
