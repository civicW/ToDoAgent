package com.asap.todoexmple.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.work.*
import java.sql.Connection
import java.sql.DriverManager
import java.util.Properties
import java.util.concurrent.ConcurrentLinkedQueue
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import java.util.concurrent.atomic.AtomicBoolean

class DatabaseHelper {
    companion object {
        private const val HOST = "103.116.245.150"
        private const val PORT = "3306"
        private const val DATABASE = "ToDoAgent"
        private const val USERNAME = "root"
        private const val PASSWORD = "4bc6bc963e6d8443453676"

        private const val POOL_SIZE = 3
        private val connectionPool = ConcurrentLinkedQueue<Connection>()
        private val connectionGuard = Any() // 添加连接保护锁
        private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        private val isInitializing = AtomicBoolean(false)
        private var applicationContext: Context? = null

        init {
            initializePool()
        }

        fun initialize(context: Context) {
            applicationContext = context.applicationContext
            initializePool()
        }

        private fun initializePool() {
            if (isInitializing.getAndSet(true)) return

            scope.launch {
                var retryCount = 0
                val maxRetries = 5
                
                while (retryCount < maxRetries && connectionPool.size < POOL_SIZE) {
                    if (!isNetworkAvailable()) {
                        delay(3000) // 等待3秒
                        continue
                    }

                    try {
                        createNewConnection()?.let { connection ->
                            if (testConnection(connection)) {
                                connectionPool.offer(connection)
                            } else {
                                try { connection.close() } catch (_: Exception) {}
                            }
                        }
                    } catch (e: Exception) {
                        retryCount++
                        delay(2000L * (retryCount + 1)) // 指数退避
                    }
                }

                isInitializing.set(false)
            }
        }

        private fun createNewConnection(): Connection? {
            return try {
                Class.forName("com.mysql.jdbc.Driver")
                val url = "jdbc:mysql://$HOST:$PORT/$DATABASE?" +
                        "useSSL=false&" +
                        "useUnicode=true&" +
                        "characterEncoding=UTF-8&" +
                        "connectionCollation=utf8mb4_unicode_ci&" +
                        "characterSetResults=UTF-8&" +
                        "autoReconnect=true&" +
                        "connectTimeout=30000&" +
                        "socketTimeout=30000&" +
                        "serverTimezone=Asia/Shanghai"

                val props = Properties().apply {
                    setProperty("user", USERNAME)
                    setProperty("password", PASSWORD)
                    setProperty("connectTimeout", "30000")
                    setProperty("socketTimeout", "30000")
                }

                DriverManager.getConnection(url, props)
            } catch (e: Exception) {
                null
            }
        }

        private fun testConnection(connection: Connection): Boolean {
            return try {
                connection.isValid(5) && connection.createStatement().use { stmt ->
                    stmt.executeQuery("SELECT 1").use { rs ->
                        rs.next()
                    }
                }
            } catch (e: Exception) {
                false
            }
        }

        private fun isNetworkAvailable(): Boolean {
            val context = applicationContext ?: return false
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork ?: return false
                val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
                return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            } else {
                @Suppress("DEPRECATION")
                return connectivityManager.activeNetworkInfo?.isConnected == true
            }
        }

        private fun startConnectionMonitor() {
            thread(start = true, isDaemon = true) {
                while (true) {
                    try {
                        synchronized(connectionGuard) {
                            val iterator = connectionPool.iterator()
                            while (iterator.hasNext()) {
                                val connection = iterator.next()
                                if (connection.isClosed || !connection.isValid(5)) {
                                    iterator.remove()
                                    createNewConnection()?.let { connectionPool.offer(it) }
                                }
                            }
                            
                            // 确保连接池保持最小连接数
                            while (connectionPool.size < POOL_SIZE) {
                                createNewConnection()?.let { connectionPool.offer(it) }
                            }
                        }
                        Thread.sleep(30000) // 每30秒检查一次
                    } catch (e: Exception) {
                        // 连接监控异常处理
                    }
                }
            }
        }

        suspend fun checkAndRepairConnections() {
            withContext(Dispatchers.IO) {
                val connectionsToAdd = mutableListOf<Connection>()
                
                synchronized(connectionGuard) {
                    try {
                        val iterator = connectionPool.iterator()
                        while (iterator.hasNext()) {
                            val connection = iterator.next()
                            try {
                                if (connection.isClosed || !connection.isValid(5)) {
                                    iterator.remove()
                                    connection.close()
                                }
                            } catch (e: Exception) {
                                iterator.remove()
                                try { connection.close() } catch (_: Exception) {}
                            }
                        }

                        val neededConnections = POOL_SIZE - connectionPool.size
                        repeat(neededConnections) {
                            createNewConnection()?.let { connectionsToAdd.add(it) }
                        }
                    } catch (e: Exception) {
                        // 检查连接失败处理
                    }
                }
                
                // 在同步块外添加新连接
                connectionsToAdd.forEach { connection ->
                    synchronized(connectionGuard) {
                        if (connectionPool.size < POOL_SIZE) {
                            connectionPool.offer(connection)
                        } else {
                            try { connection.close() } catch (_: Exception) {}
                        }
                    }
                }
            }
        }

        suspend fun getConnection(): Connection? = withContext(Dispatchers.IO) {
            for (attempt in 1..3) {
                // 如果连接池为空，尝试初始化
                if (connectionPool.isEmpty() && !isInitializing.get()) {
                    initializePool()
                }

                synchronized(connectionGuard) {
                    connectionPool.poll()?.let { connection ->
                        if (testConnection(connection)) {
                            return@withContext connection
                        }
                        try { connection.close() } catch (_: Exception) {}
                    }
                }

                // 如果没有可用连接，创建新连接
                createNewConnection()?.let { connection ->
                    if (testConnection(connection)) {
                        return@withContext connection
                    }
                    try { connection.close() } catch (_: Exception) {}
                }

                delay(2000L * attempt)
            }
            null
        }

        suspend fun releaseConnection(connection: Connection?) {
            if (connection == null) return
            
            withContext(Dispatchers.IO) {
                synchronized(connectionGuard) {
                    try {
                        if (!connection.isClosed && connection.isValid(5)) {
                            if (connectionPool.size < POOL_SIZE) {
                                connectionPool.offer(connection)
                                return@withContext
                            }
                        }
                        connection.close()
                    } catch (e: Exception) {
                        try { connection.close() } catch (_: Exception) {}
                    }
                }
            }
        }

        fun closeAllConnections() {
            synchronized(connectionGuard) {
                connectionPool.forEach { conn ->
                    try { conn.close() } catch (_: Exception) {}
                }
                connectionPool.clear()
                scope.cancel()
            }
        }

        fun setupBackgroundWork(context: Context) {
            val workRequest = PeriodicWorkRequestBuilder<DatabaseWorker>(
                15, TimeUnit.MINUTES,
                5, TimeUnit.MINUTES
            ).setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            ).build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    "database_sync",
                    ExistingPeriodicWorkPolicy.KEEP,
                    workRequest
                )
        }
    }
}

class DatabaseWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        val powerManager = applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "ASAP:DatabaseWorkerWakeLock"
        )
        
        wakeLock.acquire(10 * 60 * 1000L) // 10分钟超时
        
        try {
            DatabaseHelper.checkAndRepairConnections()
            return Result.success()
            } catch (e: Exception) {
            return Result.retry()
        } finally {
            if (wakeLock.isHeld) {
                wakeLock.release()
            }
        }
    }
}
