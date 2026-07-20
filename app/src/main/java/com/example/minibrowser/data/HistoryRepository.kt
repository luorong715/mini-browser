package com.example.minibrowser.data

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.flow.Flow

class HistoryRepository(context: Context) {

    private val dao = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "minibrowser.db"
    ).build().historyDao()

    suspend fun addHistory(url: String, title: String) {
        // 过滤无效 URL
        if (url.isBlank()) return
        dao.insert(HistoryEntity(url = url, title = title, timestamp = System.currentTimeMillis()))
    }

    fun getHistoryFlow(): Flow<List<HistoryEntity>> = dao.getAllHistory()

    suspend fun deleteHistory(id: Long) = dao.deleteById(id)

    suspend fun clearAll() = dao.clearAll()
}
