package com.asap.todoexmple.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.asap.todoexmple.R
import com.asap.todoexmple.activity.SettingsActivity

class ProfileFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // 初始化个人中心页面的视图和数据

        // 设置按钮点击事件
        view.findViewById<View>(R.id.settingsFragment).setOnClickListener {
            // 启动设置Activity
            startActivity(Intent(requireContext(), SettingsActivity::class.java))
        }
    }
} 