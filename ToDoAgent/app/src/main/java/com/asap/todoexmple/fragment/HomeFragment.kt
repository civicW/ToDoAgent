package com.asap.todoexmple.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.asap.todoexmple.R

class HomeFragment : Fragment() {
    // 将按钮声明为可空类型，避免 lateinit 初始化问题
    private var btnAll: Button? = null
    private var btnToday: Button? = null
    private var btnImportant: Button? = null
    private var btnCompleted: Button? = null
    private var taskList: RecyclerView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        setupTaskList()
        setupCategoryButtons()
        // 默认选中"今天"按钮
        btnToday?.let { button ->
            resetButtonStyles(listOfNotNull(btnAll, btnToday, btnImportant, btnCompleted))
            setSelectedButtonStyle(button)
        }
    }

    private fun initViews(view: View) {
        btnAll = view.findViewById(R.id.btnAll)
        btnToday = view.findViewById(R.id.btnToday)
        btnImportant = view.findViewById(R.id.btnImportant)
        btnCompleted = view.findViewById(R.id.btnCompleted)
        taskList = view.findViewById(R.id.taskList)
    }

    private fun setupTaskList() {
        taskList?.layoutManager = LinearLayoutManager(context)
        // TODO: 设置任务列表适配器
    }

    private fun setupCategoryButtons() {
        val buttons = listOfNotNull(btnAll, btnToday, btnImportant, btnCompleted)
        buttons.forEach { button ->
            button.setOnClickListener {
                resetButtonStyles(buttons)
                setSelectedButtonStyle(button)
                handleButtonClick(button)
            }
        }
    }

    private fun resetButtonStyles(buttons: List<Button>) {
        buttons.forEach { button ->
            button.setBackgroundResource(R.drawable.button_rounded)
            button.setTextColor(requireContext().getColor(android.R.color.darker_gray))
            button.backgroundTintList = android.content.res.ColorStateList.valueOf(
                requireContext().getColor(android.R.color.white)
            )
        }
    }

    private fun setSelectedButtonStyle(button: Button) {
        button.backgroundTintList = android.content.res.ColorStateList.valueOf(
            requireContext().getColor(R.color.blue_500) // 使用统一的蓝色
        )
        button.setTextColor(requireContext().getColor(android.R.color.white))
    }

    private fun handleButtonClick(button: Button) {
        when (button.id) {
            R.id.btnAll -> {
                // TODO: 处理"全部"按钮点击
            }
            R.id.btnToday -> {
                // TODO: 处理"今天"按钮点击
            }
            R.id.btnImportant -> {
                // TODO: 处理"重要"按钮点击
            }
            R.id.btnCompleted -> {
                // TODO: 处理"已完成"按钮点击
            }
        }
    }
} 