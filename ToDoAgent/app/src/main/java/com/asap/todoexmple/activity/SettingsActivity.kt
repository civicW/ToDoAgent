package com.asap.todoexmple.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.app.ActivityManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.asap.todoexmple.R
import com.asap.todoexmple.receiver.KeepAliveUtils
import android.widget.Toast
import android.widget.TextView
import android.widget.ImageView
import com.asap.todoexmple.util.UserManager

class SettingsActivity : AppCompatActivity() {

    @SuppressLint("BatteryLife")
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // 返回按钮点击事件
        findViewById<View>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // 设置用户信息区域点击事件
        setupUserInfoSection()

        // 这里添加其他设置项的点击事件处理
        findViewById<View>(R.id.layoutNotification).setOnClickListener {
            // 处理通知设置点击
        }

        findViewById<View>(R.id.layoutLanguage).setOnClickListener {
            // 处理语言设置点击
        }

        findViewById<View>(R.id.layoutHelp).setOnClickListener {
            // 处理帮助与反馈点击
        }

        findViewById<View>(R.id.layoutAbout).setOnClickListener {
            // 处理关于我们点击
        }

        findViewById<View>(R.id.layoutLogout).setOnClickListener {
            // 处理退出登录点击
        }

        // 保活措施相关
        val layoutKeepAliveHeader = findViewById<View>(R.id.layoutKeepAliveHeader)
        val layoutKeepAliveOptions = findViewById<View>(R.id.layoutKeepAliveOptions)
        val imgKeepAliveArrow = findViewById<View>(R.id.imgKeepAliveArrow)
        
        // 开关控件
        val switchBootStart = findViewById<SwitchCompat>(R.id.switchBootStart)
        val switchBatteryOptimization = findViewById<SwitchCompat>(R.id.switchBatteryOptimization)
        val switchHideRecents = findViewById<SwitchCompat>(R.id.switchHideRecents)

        // 初始化开关状态
        switchBootStart.isChecked = KeepAliveUtils.isBootStartEnabled(this)
        switchBatteryOptimization.isChecked = KeepAliveUtils.isIgnoringBatteryOptimizations(this)
        switchHideRecents.isChecked = KeepAliveUtils.isHiddenFromRecents(this)

        // 点击展开/收起
        layoutKeepAliveHeader.setOnClickListener {
            if (layoutKeepAliveOptions.visibility == View.VISIBLE) {
                layoutKeepAliveOptions.visibility = View.GONE
                imgKeepAliveArrow.rotation = 0f
            } else {
                layoutKeepAliveOptions.visibility = View.VISIBLE
                imgKeepAliveArrow.rotation = 90f
            }
        }

        // 开关事件处理
        switchBootStart.setOnCheckedChangeListener { _, isChecked ->
            KeepAliveUtils.setBootStartEnabled(this, isChecked)
            KeepAliveUtils.setBackgroundStartEnabled(this, isChecked)
            if (isChecked) {
                Toast.makeText(this, "已开启开机自启动和后台自启动", Toast.LENGTH_SHORT).show()
            }
        }

        switchBatteryOptimization.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        }

        switchHideRecents.setOnCheckedChangeListener { _, isChecked ->
            KeepAliveUtils.setHiddenFromRecents(this, isChecked)
            if (isChecked) {
                setTaskDescription(ActivityManager.TaskDescription.Builder()
                    .setLabel("")  // 空标签
                    .build())
            }
        }
    }

    private fun setupUserInfoSection() {
        val layoutUserInfo = findViewById<View>(R.id.layoutUserInfo)
        val txtLoginStatus = findViewById<TextView>(R.id.txtLoginStatus)
        val txtUserInfo = findViewById<TextView>(R.id.txtUserInfo)
        val imgAvatar = findViewById<ImageView>(R.id.imgAvatar)

        val isLoggedIn = UserManager.isLoggedIn(this)
        
        if (isLoggedIn) {
            txtLoginStatus.text = UserManager.getUserName(this)
            txtUserInfo.visibility = View.VISIBLE
            txtUserInfo.text = "ID: ${UserManager.getUserId(this)}"
        } else {
            txtLoginStatus.text = "登录/注册"
            txtUserInfo.visibility = View.GONE
        }

        layoutUserInfo.setOnClickListener {
            if (!isLoggedIn) {
                startActivity(Intent(this, LoginActivity::class.java))
            }
        }

        findViewById<View>(R.id.layoutAccountSecurity).visibility = 
            if (isLoggedIn) View.VISIBLE else View.GONE
    }
} 