package com.example.minibrowser

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// ✅ 1. 定义 DataStore 实例（全局单例）
private val Context.skinDataStore: DataStore<Preferences> by preferencesDataStore(name = "skin_prefs")

// ✅ 2. 定义存储 Key
private val SKIN_TYPE_KEY = stringPreferencesKey("current_skin_type")

object SkinPreferences {

    // ✅ 3. 读取皮肤状态（返回 Flow，支持响应式更新）
    fun getSkinFlow(context: Context): Flow<SkinType> {
        return context.skinDataStore.data.map { prefs ->
            val skinName = prefs[SKIN_TYPE_KEY] ?: SkinType.DEFAULT.name
            try {
                SkinType.valueOf(skinName)
            } catch (e: IllegalArgumentException) {
                SkinType.DEFAULT
            }
        }
    }

    // ✅ 4. 保存皮肤状态
    suspend fun saveSkin(context: Context, skin: SkinType) {
        context.skinDataStore.edit { prefs ->
            prefs[SKIN_TYPE_KEY] = skin.name
        }
    }
}
