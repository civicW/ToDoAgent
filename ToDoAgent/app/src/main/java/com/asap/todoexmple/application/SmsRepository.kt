package com.asap.todoexmple.application

import android.util.Log
import com.asap.todoexmple.service.BaseRepository



class SmsRepository : BaseRepository() {
    suspend fun saveSmsData(sender: String?, content: String?, messageId : Int): Boolean {
        try {
            return executeDbOperation { connection ->
                Log.d("SmsRepository", "开始保存短信数据: sender=$sender")
                val sql = "INSERT INTO Messages (sender, content, user_id , message_id) VALUES (?, ?, '1' , ?)"
                
                connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, sender ?: "无标题")
                    stmt.setString(2, content ?: "无内容")
                    stmt.setInt(3, messageId)
                    val result = stmt.executeUpdate() > 0
                    Log.d("SmsRepository", "短信数据保存结果: $result")
                    result
                }
            }
        } catch (e: Exception) {
            Log.e("SmsRepository", "保存短信数据失败", e)
            throw e
        }
    }
}

