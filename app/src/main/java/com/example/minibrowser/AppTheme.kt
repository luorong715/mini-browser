package com.example.minibrowser

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// 1. 定义皮肤枚举
enum class SkinType(val displayName: String) {
    DEFAULT("默认"),
    OCEAN("海洋蓝"),
    SUNSET("日落橘"),
    DARK("暗夜黑")
}

// 2. 定义各皮肤对应的 ColorScheme
fun SkinType.toColorScheme(isSystemDark: Boolean): androidx.compose.material3.ColorScheme {
    return when (this) {
        SkinType.DEFAULT -> if (isSystemDark) darkColorScheme() else lightColorScheme()

        SkinType.OCEAN -> if (isSystemDark) {
            darkColorScheme(primary = Color(0xFF4FC3F7), secondary = Color(0xFF81D4FA))
        } else {
            lightColorScheme(primary = Color(0xFF0288D1), secondary = Color(0xFF039BE5))
        }

        SkinType.SUNSET -> if (isSystemDark) {
            darkColorScheme(primary = Color(0xFFFFAB91), secondary = Color(0xFFFFCCBC))
        } else {
            lightColorScheme(primary = Color(0xFFE64A19), secondary = Color(0xFFF4511E))
        }

        SkinType.DARK -> darkColorScheme(
            primary = Color(0xFFBB86FC),
            background = Color(0xFF121212),
            surface = Color(0xFF1E1E1E)
        )
    }
}
