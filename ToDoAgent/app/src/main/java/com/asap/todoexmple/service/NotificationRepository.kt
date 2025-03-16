package com.asap.todoexmple.service

import android.content.Context
import android.util.Log
import com.asap.todoexmple.util.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class NotificationRepository : BaseRepository() {
    suspend fun saveNotification(context: Context, sender: String?, content: String?, messageId: Int): Boolean {
        if (sender == null && content == null) {
            Log.d("NotificationRepository", "标题和内容都为空，跳过保存")
            return false
        }

        val userId = SessionManager.Session.getUserId(context) ?: return false

        return withContext(Dispatchers.IO) {
            var success = false
            for (i in 1..3) { // 最多重试3次
                success = executeDbOperation { connection ->
                    val sql = "INSERT INTO Messages (sender, content, user_id, message_id) VALUES (?, ?, ?, ?)"
                    connection.prepareStatement(sql).use { stmt ->
                        stmt.setString(1, sender ?: "无标题")
                        stmt.setString(2, content ?: "无内容")
                        stmt.setString(3, userId)
                        stmt.setInt(4, messageId)
                        
                        val result = stmt.executeUpdate() > 0
                        if (!result) {
                            Log.e("NotificationRepository", "插入失败，尝试次数：$i")
                        }
                        result
                    }
                }
                if (success) break
                delay(1000L * i) // 延迟重试
            }
            success
        }
    }
}
