package com.example.minibrowser.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    // 插入新记录，如果 URL 完全相同则替换（避免重复堆积）
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: HistoryEntity)

    // 按时间倒序获取所有历史记录，返回 Flow 以支持 UI 自动刷新
    @Query("SELECT * FROM history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<HistoryEntity>>

    // 根据 ID 删除单条记录（为后续的滑动/长按删除做准备）
    @Query("DELETE FROM history WHERE id = :id")
    suspend fun deleteById(id: Long)

    // 清空全部历史
    @Query("DELETE FROM history")
    suspend fun clearAll()
}
