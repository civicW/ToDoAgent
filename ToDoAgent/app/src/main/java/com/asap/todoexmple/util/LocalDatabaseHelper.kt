package com.asap.todoexmple.util

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocalDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        private const val TAG = "LocalDatabaseHelper"
        private const val DATABASE_NAME = "user_settings.db"
        private const val DATABASE_VERSION = 3  // 将版本号从2改为3

        // SQL 语句常量
        private const val CREATE_USER_SETTINGS_TABLE = """
            CREATE TABLE IF NOT EXISTS user_settings (
                user_id TEXT PRIMARY KEY,
                keep_alive_boot INTEGER DEFAULT 0,
                keep_alive_battery INTEGER DEFAULT 0,
                keep_alive_hidden INTEGER DEFAULT 0,
                dark_mode INTEGER DEFAULT 0,
                language TEXT DEFAULT 'zh'
            )
        """

        private const val CREATE_TODO_LIST_TABLE = """
         CREATE TABLE IF NOT EXISTS ToDoListLocal (
            list_id TEXT PRIMARY KEY,
            user_id TEXT NOT NULL,
            start_time TEXT,
            end_time TEXT,
            location TEXT,
            todo_content TEXT,
            is_important INTEGER DEFAULT 0,
            is_completed INTEGER DEFAULT 0,
            sync_status INTEGER DEFAULT 0,
            last_modified TEXT,
            FOREIGN KEY (user_id) REFERENCES user_settings(user_id) ON DELETE CASCADE
        )
        """

        // 添加设置定期同步的方法
        fun setupPeriodicSync(context: Context, userId: String) {
            try {
                val syncData = androidx.work.Data.Builder()
                    .putString("user_id", userId)
                    .build()

                val syncRequest = androidx.work.PeriodicWorkRequestBuilder<ToDoSyncWorker>(
                    10, java.util.concurrent.TimeUnit.MINUTES,
                    5, java.util.concurrent.TimeUnit.MINUTES
                )
                    .setInputData(syncData)
                    .setConstraints(
                        androidx.work.Constraints.Builder()
                            .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                            .build()
                    )
                    .addTag("todo_sync")  // 添加标签便于管理
                    .build()

                androidx.work.WorkManager.getInstance(context)
                    .enqueueUniquePeriodicWork(
                        "todo_sync_$userId",
                        androidx.work.ExistingPeriodicWorkPolicy.REPLACE,
                        syncRequest
                    )
                
                Log.d(TAG, "已设置待办事项定期同步任务")
            } catch (e: Exception) {
                Log.e(TAG, "设置定期同步失败", e)
            }
        }

        // 添加立即同步方法
        fun startImmediateSync(context: Context, userId: String) {
            try {
                val syncData = androidx.work.Data.Builder()
                    .putString("user_id", userId)
                    .build()

                val syncRequest = androidx.work.OneTimeWorkRequestBuilder<ToDoSyncWorker>()
                    .setInputData(syncData)
                    .setConstraints(
                        androidx.work.Constraints.Builder()
                            .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                            .build()
                    )
                    .addTag("todo_sync_immediate")
                    .build()

                androidx.work.WorkManager.getInstance(context)
                    .enqueueUniqueWork(
                        "todo_immediate_sync_$userId",
                        androidx.work.ExistingWorkPolicy.REPLACE,
                        syncRequest
                    )
                
                Log.d(TAG, "已触发立即同步")
            } catch (e: Exception) {
                Log.e(TAG, "触发立即同步失败", e)
            }
        }

        // 取消同步
        fun cancelSync(context: Context, userId: String) {
            try {
                androidx.work.WorkManager.getInstance(context).apply {
                    cancelUniqueWork("todo_sync_$userId")
                    cancelUniqueWork("todo_immediate_sync_$userId")
                }
                Log.d(TAG, "已取消所有同步任务")
            } catch (e: Exception) {
                Log.e(TAG, "取消同步任务失败", e)
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        try {
            db.beginTransaction()
            try {
                // 创建表
                db.execSQL(CREATE_USER_SETTINGS_TABLE)
                db.execSQL(CREATE_TODO_LIST_TABLE)
                
                // 创建索引提高查询性能
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_todo_user_id ON ToDoListLocal(user_id)")
                
                db.setTransactionSuccessful()
                Log.d(TAG, "数据库表创建成功")
            } finally {
                db.endTransaction()
            }
        } catch (e: Exception) {
            Log.e(TAG, "创建数据库失败", e)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        try {
            db.beginTransaction()
            try {
                // 检查表是否存在
                val cursor = db.rawQuery(
                    "SELECT name FROM sqlite_master WHERE type='table' AND name='ToDoListLocal'",
                    null
                )
                val tableExists = cursor.count > 0
                cursor.close()

                if (tableExists) {
                    // 如果表存在，添加新列
                    if (oldVersion < 3) {
                        // 添加 is_important 和 is_completed 列
                        db.execSQL("ALTER TABLE ToDoListLocal ADD COLUMN is_important INTEGER DEFAULT 0")
                        db.execSQL("ALTER TABLE ToDoListLocal ADD COLUMN is_completed INTEGER DEFAULT 0")
                    }
                } else {
                    // 如果表不存在，创建新表
                    db.execSQL(CREATE_TODO_LIST_TABLE)
                    db.execSQL("CREATE INDEX IF NOT EXISTS idx_todo_user_id ON ToDoListLocal(user_id)")
                }
                
                db.setTransactionSuccessful()
                Log.d(TAG, "数据库升级成功")
            } finally {
                db.endTransaction()
            }
        } catch (e: Exception) {
            Log.e(TAG, "数据库升级失败: ${e.message}")
            // 确保表存在
            try {
                db.execSQL(CREATE_TODO_LIST_TABLE)
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_todo_user_id ON ToDoListLocal(user_id)")
                Log.d(TAG, "已创建新的ToDoListLocal表")
            } catch (e: Exception) {
                Log.e(TAG, "创建新表失败: ${e.message}")
            }
        }
    }

    // 优化的同步方法
    suspend fun syncToDoListFromCloud(userId: String) = withContext(Dispatchers.IO) {
        try {
            // 验证用户ID
            if (!isValidUser(userId)) {
                Log.e(TAG, "同步失败：用户ID不存在")
                return@withContext
            }

            val dbHelper = DatabaseHelper.Companion
            val connection = dbHelper.getConnection() ?: return@withContext

            try {
                // 获取最后同步时间
                val lastSyncTime = getLastSyncTime(userId)
                
                // 获取云端数据
                connection.prepareStatement("""
                    SELECT list_id, user_id, start_time, end_time, location, todo_content 
                    FROM ToDoList 
                    WHERE user_id = ? AND last_modified > ?
                """).use { stmt ->
                    stmt.setString(1, userId)
                    stmt.setString(2, lastSyncTime)
                    val rs = stmt.executeQuery()

                    val db = writableDatabase
                    db.beginTransaction()
                    try {
                        // 更新本地数据
                        while (rs.next()) {
                            updateLocalTodoItem(db, rs)
                        }
                        
                        // 更新同步时间
                        updateLastSyncTime(db, userId)
                        
                        db.setTransactionSuccessful()
                        Log.d(TAG, "同步成功：用户ID = $userId")
                    } finally {
                        db.endTransaction()
                    }
                }
            } finally {
                dbHelper.releaseConnection(connection)
            }
        } catch (e: Exception) {
            Log.e(TAG, "同步待办事项失败", e)
        }
    }

    // 辅助方法
    private fun isValidUser(userId: String): Boolean {
        return readableDatabase.query(
            "user_settings",
            arrayOf("user_id"),
            "user_id = ?",
            arrayOf(userId),
            null,
            null,
            null
        ).use { cursor ->
            cursor.moveToFirst()
        }
    }

    private fun getLastSyncTime(userId: String): String {
        // 从preferences或数据库获取最后同步时间
        return "1970-01-01 00:00:00"  // 默认值
    }

    private fun updateLocalTodoItem(db: SQLiteDatabase, rs: java.sql.ResultSet) {
        val values = android.content.ContentValues().apply {
            put("list_id", rs.getString("list_id"))
            put("user_id", rs.getString("user_id"))
            put("start_time", rs.getString("start_time"))
            put("end_time", rs.getString("end_time"))
            put("location", rs.getString("location"))
            put("todo_content", rs.getString("todo_content"))
            put("sync_status", 1)
            put("last_modified", System.currentTimeMillis().toString())
        }
        
        db.insertWithOnConflict(
            "ToDoListLocal",
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    private fun updateLastSyncTime(db: SQLiteDatabase, userId: String) {
        // 更新最后同步时间
    }
}

// 添加同步工作器类
class ToDoSyncWorker(
    context: android.content.Context,
    params: androidx.work.WorkerParameters
) : androidx.work.CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        try {
            val userId = inputData.getString("user_id") ?: return Result.failure()
            val dbHelper = LocalDatabaseHelper(applicationContext)
            
            // 首先验证用户ID是否存在于user_settings表中
            val db = dbHelper.readableDatabase
            val cursor = db.query(
                "user_settings",
                arrayOf("user_id"),
                "user_id = ?",
                arrayOf(userId),
                null,
                null,
                null
            )
            
            if (!cursor.moveToFirst()) {
                android.util.Log.e("ToDoSyncWorker", "用户ID在本地数据库中不存在")
                cursor.close()
                return Result.failure()
            }
            cursor.close()
            
            // 确认是合法用户后执行同步
            dbHelper.syncToDoListFromCloud(userId)
            
            android.util.Log.d("ToDoSyncWorker", "待办事项同步成功")
            return Result.success()
        } catch (e: Exception) {
            android.util.Log.e("ToDoSyncWorker", "同步失败: ${e.message}")
            return Result.retry()
        }
    }
}

// 在用户登出或需要停止同步时调用
//  LocalDatabaseHelper.cancelPeriodicSync(context, userId)
//勿删除，取消同步方法