package com.asap.todoexmple.application

import com.asap.todoexmple.service.BaseRepository



class SmsRepository : BaseRepository() {
    suspend fun saveSmsData(sender: String?, content: String?, messageId : Int): Boolean {
        return executeDbOperation { connection ->
            val sql = "INSERT INTO Messages (sender, content, user_id , message_id) VALUES (?, ?, '1' , ?)"

            connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, sender ?: "无标题")
                stmt.setString(2, content ?: "无内容")
                stmt.setInt(3, messageId )
                stmt.executeUpdate() > 0
            }
        }
    }
}

