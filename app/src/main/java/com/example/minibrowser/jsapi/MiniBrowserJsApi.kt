package com.example.minibrowser.jsapi

import android.webkit.JavascriptInterface
import org.json.JSONArray
import org.json.JSONObject
import com.example.minibrowser.data.HistoryEntity

/**
 * 1.4 JSAPI：供 H5 调用的原生接口
 * H5 端通过 window.MiniBrowser.getHistory() 同步获取历史记录
 */
class MiniBrowserJsApi(
    private val getHistorySync: () -> List<HistoryEntity>
) {
    @JavascriptInterface
    fun getHistory(): String {
        return try {
            val records = getHistorySync()
            val jsonArray = JSONArray()
            records.forEach { record ->
                jsonArray.put(JSONObject().apply {
                    put("url", record.url)
                    put("title", record.title)
                    put("timestamp", record.timestamp)
                })
            }
            jsonArray.toString()
        } catch (e: Exception) {
            "[]" // 异常兜底，防止 H5 解析崩溃
        }
    }
}
