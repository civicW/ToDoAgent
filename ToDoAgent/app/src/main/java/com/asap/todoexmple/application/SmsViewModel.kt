package com.asap.todoexmple.application

import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import android.app.Application
import com.asap.todoexmple.service.SmsHandler
import com.asap.todoexmple.receiver.KeepAliveUtils
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class SmsMessage(
    val sender: String,
    val body: String,
    val timestamp: Long
)

// 将 ViewModel 改为 AndroidViewModel
class SmsViewModel(application: Application) : AndroidViewModel(application) {
    private val _smsFlow = MutableSharedFlow<SmsMessage>()
    val smsFlow = _smsFlow.asSharedFlow()
    private val smsHandler = SmsHandler.getInstance()
    private val context: Context = application.applicationContext

    suspend fun sendSms(sender: String, body: String, timestamp: Long) {
        try {
            // 检查是否启用了后台自启动
            if (KeepAliveUtils.isBackgroundStartEnabled(context)) {
                // 发送到 Flow
                _smsFlow.emit(SmsMessage(sender, body, timestamp))
                
                // 处理短信
                smsHandler.handleSmsMessage(sender, body, timestamp)
            }
        } catch (e: Exception) {
            android.util.Log.e("SmsViewModel", "处理短信失败", e)
            throw e
        }
    }

    // 添加 ViewModelFactory
    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SmsViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return SmsViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
