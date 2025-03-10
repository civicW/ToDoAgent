package com.asap.todoexmple.application


import android.util.Log

import com.asap.todoexmple.util.DatabaseHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext



class SmsRepository {
    suspend fun saveSmsData(sender: String?, content: String?): Boolean = withContext(Dispatchers.IO) {
        var connection: java.sql.Connection? = null

        try {
            connection = DatabaseHelper.getConnection()

            // 设置连接的字符编码为 UTF-8
            connection?.createStatement()?.use { stmt ->
                stmt.execute("SET NAMES utf8mb4")
                stmt.execute("SET CHARACTER SET utf8mb4")
                stmt.execute("SET character_set_client = utf8mb4")
                stmt.execute("SET character_set_connection = utf8mb4")
                stmt.execute("SET character_set_results = utf8mb4")
            }

            val sql = "INSERT INTO Messages (sender, content) VALUES (?, ?)"
            connection?.prepareStatement(sql)?.use { stmt ->
                stmt.setBytes(1, (sender ?: "无标题").toByteArray(Charsets.UTF_8))
                stmt.setBytes(2, (content ?: "无内容").toByteArray(Charsets.UTF_8))
                //stmt.setString(3, timestamp)

                // 打印实际插入的值
                Log.d("SmsRepository", "准备插入: sender=$sender, content=$content")

                return@withContext stmt.executeUpdate() > 0
            }
            false
        } catch (e: Exception) {
            Log.e("SmsRepository", "保存失败: ${e.message}")
            e.printStackTrace()
            false
        } finally {
            connection?.close()
        }
    }
}

