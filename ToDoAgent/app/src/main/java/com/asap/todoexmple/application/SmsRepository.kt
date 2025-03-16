package com.asap.todoexmple.application

import android.content.Context
import android.util.Log
import com.asap.todoexmple.service.BaseRepository
import com.asap.todoexmple.util.SessionManager

class SmsRepository : BaseRepository() {
    suspend fun saveSmsData(context: Context, sender: String?, content: String?, messageId: Int): Boolean {
        try {
            val userId = SessionManager.Session.getUserId(context) ?: return false
            
            return executeDbOperation { connection ->
                Log.d("SmsRepository", "开始保存短信数据: sender=$sender, userId=$userId")
                val sql = "INSERT INTO Messages (sender, content, user_id, message_id) VALUES (?, ?, ?, ?)"
                
                connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, sender ?: "无标题")
                    stmt.setString(2, content ?: "无内容")
                    stmt.setString(3, userId)
                    stmt.setInt(4, messageId)
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

