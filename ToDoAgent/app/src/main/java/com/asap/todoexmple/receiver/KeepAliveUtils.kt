package com.asap.todoexmple.receiver

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.PowerManager

object KeepAliveUtils {
    private const val PREFS_NAME = "keep_alive_settings"
    private const val KEY_BOOT_START = "boot_start_enabled"
    private const val KEY_BACKGROUND_START = "background_start_enabled"
    private const val KEY_HIDE_RECENTS = "hide_from_recents"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isBootStartEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_BOOT_START, false)
    }

    fun setBootStartEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_BOOT_START, enabled).apply()
    }

    fun isBackgroundStartEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_BACKGROUND_START, false)
    }

    fun setBackgroundStartEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_BACKGROUND_START, enabled).apply()
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun isHiddenFromRecents(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_HIDE_RECENTS, false)
    }

    fun setHiddenFromRecents(context: Context, hidden: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_HIDE_RECENTS, hidden).apply()
    }

    fun startForegroundService(context: Context, serviceClass: Class<*>) {
        val intent = Intent(context, serviceClass)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun restartService(context: Context, serviceClass: Class<*>) {
        context.stopService(Intent(context, serviceClass))
        startForegroundService(context, serviceClass)
    }

    fun requestIgnoreBatteryOptimizations(context: Context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            val intent = Intent()
            intent.action = android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
            intent.data = android.net.Uri.parse("package:${context.packageName}")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
} 