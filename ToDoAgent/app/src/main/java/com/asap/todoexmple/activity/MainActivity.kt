package com.asap.todoexmple.activity

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import android.view.View

import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.asap.todoexmple.R
import com.asap.todoexmple.application.SmsRepository
import com.asap.todoexmple.application.SmsViewModel
import com.asap.todoexmple.application.YourApplication
import com.asap.todoexmple.service.NotificationMonitorService
import kotlinx.coroutines.launch
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import com.asap.todoexmple.service.SmsHandler
import android.app.ActivityManager
import android.net.Uri
import android.provider.Settings
import com.asap.todoexmple.receiver.KeepAliveUtils
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider

class MainActivity : AppCompatActivity() {

    private lateinit var smsViewModel: SmsViewModel
    private val smsRepository = SmsRepository()
    private val smsHandler = SmsHandler.getInstance()
    private val PERMISSION_REQUEST_CODE = 123
    private val NOTIFICATION_PERMISSION_REQUEST_CODE = 456

    // 权限请求
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (!permissions.all { it.value }) {
            Toast.makeText(this, "需要短信权限才能接收短信", Toast.LENGTH_SHORT).show()
        }
    }

/////////////////android 生命周期之onCreat//////////////////////////
   @RequiresApi(Build.VERSION_CODES.TIRAMISU)
   override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //enableEdgeToEdge()

        setContentView(R.layout.activity_main)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // 设置底部导航
        findViewById<BottomNavigationView>(R.id.bottom_nav_view)
            .setupWithNavController(navController)

        // 添加导航监听
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val searchBar = findViewById<View>(R.id.searchBar)
            val categoryScroll = findViewById<View>(R.id.categoryScroll)
            val taskList = findViewById<View>(R.id.taskList)

            when (destination.id) {
                R.id.navigation_statistics, R.id.navigation_profile -> {
                    // 统计和我的页面隐藏搜索栏、类别栏和任务列表
                    searchBar.visibility = View.GONE
                    categoryScroll.visibility = View.GONE
                    taskList.visibility = View.GONE
                }
                else -> {
                    // 其他页面显示搜索栏、类别栏和任务列表
                    searchBar.visibility = View.VISIBLE
                    categoryScroll.visibility = View.VISIBLE
                    taskList.visibility = View.VISIBLE
                }
            }
        }

        // 通知监听服务初始化
        initNotificationService()

        // 初始化 ViewModel
        smsViewModel = ViewModelProvider(this)[SmsViewModel::class.java]

        // 监听短信流
        lifecycleScope.launch {
            smsViewModel.smsFlow.collect { smsMessage ->
                Log.d("MainActivity", "收到短信: ${smsMessage.sender} - ${smsMessage.body}")
            }
        }

        // 检查并请求权限
        checkAndRequestPermissions()
        
        // 检查通知监听权限
        if (!isNotificationListenerEnabled()) {
            requestNotificationListenerPermission()
        }

        // 观察短信更新
        observeSmsUpdates()

        // 检查是否需要隐藏最近任务
        if (KeepAliveUtils.isHiddenFromRecents(this)) {
            setTaskDescription(ActivityManager.TaskDescription.Builder()
                .setLabel("")  // 空标签
                .build())
        }

        // 初始化保活措施
        initKeepAlive()
    }


/////////////////自定义的函数//////////////////////////
    private fun observeSmsUpdates() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                smsViewModel.smsFlow.collect { smsMessage ->
                    smsHandler.handleSmsMessage(
                        smsMessage.sender,
                        smsMessage.body,
                        smsMessage.timestamp
                    )
                }
            }
        }
    }

    //////通知获取的函数小伙伴们/////
    private fun initNotificationService() {
        // 修改通知监听服务的启动逻辑
        if (!isNotificationServiceEnabled()) {
            // 如果服务未启用，引导用户去设置
            Toast.makeText(this, "请授权通知访问权限", Toast.LENGTH_LONG).show()
            startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
        } else {
            // 服务已启用，确保服务正在运行
            toggleNotificationListenerService()
        }
    }

    // 添加切换服务的方法
    private fun toggleNotificationListenerService() {
        val thisComponent = ComponentName(this, NotificationMonitorService::class.java)
        packageManager.setComponentEnabledSetting(
            thisComponent,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
        packageManager.setComponentEnabledSetting(
            thisComponent,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )

        // 重启服务
        val intent = Intent(this, NotificationMonitorService::class.java)
        stopService(intent)
        startService(intent)
    }

    // 添加检查通知监听服务是否启用的方法
    private fun isNotificationServiceEnabled(): Boolean {
        val packageNames = NotificationManagerCompat.getEnabledListenerPackages(this)
        return packageNames.contains(packageName)
    }
    //////通知获取的函数小伙伴们--到这里结束啦/////

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf<String>()

        // 检查短信权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS)
            != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.RECEIVE_SMS)
        }

        // Android 13及以上需要请求通知权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // 如果有需要请求的权限，则请求
        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissions.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val packageName = packageName
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return flat?.contains(packageName) == true
    }

    private fun requestNotificationListenerPermission() {
        try {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        } catch (e: Exception) {
            Log.e("MainActivity", "打开通知监听设置失败", e)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty()) {
                    for (i in permissions.indices) {
                        val permission = permissions[i]
                        val granted = grantResults[i] == PackageManager.PERMISSION_GRANTED
                        Log.d("MainActivity", "权限 $permission ${if (granted) "已授予" else "被拒绝"}")
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 检查通知监听服务是否启用
        if (!isNotificationListenerEnabled()) {
            requestNotificationListenerPermission()
        }
    }

    private fun initKeepAlive() {
        if (!KeepAliveUtils.isIgnoringBatteryOptimizations(this)) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        }
    }
}



