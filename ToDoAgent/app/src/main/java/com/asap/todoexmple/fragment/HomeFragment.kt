package com.asap.todoexmple.fragment

import Task
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.asap.todoexmple.R

import com.asap.todoexmple.adapter.TaskAdapter
import com.asap.todoexmple.util.LocalDatabaseHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : Fragment() {
    // 将按钮声明为可空类型，避免 lateinit 初始化问题
    private var btnAll: Button? = null
    private var btnToday: Button? = null
    private var btnImportant: Button? = null
    private var btnCompleted: Button? = null
    private var taskList: RecyclerView? = null
    private var taskAdapter: TaskAdapter? = null
    private lateinit var dbHelper: LocalDatabaseHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dbHelper = LocalDatabaseHelper(requireContext())
        initViews(view)
        setupTaskList()
        setupCategoryButtons()
        // 默认选中"今天"按钮
        btnToday?.let { button ->
            resetButtonStyles(listOfNotNull(btnAll, btnToday, btnImportant, btnCompleted))
            setSelectedButtonStyle(button)
            loadTodayTasks() // 加载今天的任务
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
        taskAdapter = TaskAdapter()
        taskList?.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = taskAdapter
        }
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
            //button.setBackgroundResource(R.drawable.`button_rounded.xml`)
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
            R.id.btnAll -> loadAllTasks()
            R.id.btnToday -> loadTodayTasks()
            R.id.btnImportant -> loadImportantTasks()
            R.id.btnCompleted -> loadCompletedTasks()
        }
    }

    private fun loadAllTasks() {
        lifecycleScope.launch(Dispatchers.IO) {
            val cursor = dbHelper.readableDatabase.query(
                "ToDoListLocal",
                null,
                null,
                null,
                null,
                null,
                "start_time DESC"
            )
            
            val tasks = mutableListOf<Task>()
            cursor.use {
                while (it.moveToNext()) {
                    tasks.add(cursorToTask(it))
                }
            }
            
            withContext(Dispatchers.Main) {
                taskAdapter?.updateTasks(tasks)
            }
        }
    }

    private fun loadTodayTasks() {
        lifecycleScope.launch(Dispatchers.IO) {
            val today = java.time.LocalDate.now().toString()
            val cursor = dbHelper.readableDatabase.query(
                "ToDoListLocal",
                null,
                "date(start_time) = ?",
                arrayOf(today),
                null,
                null,
                "start_time ASC"
            )
            
            val tasks = mutableListOf<Task>()
            cursor.use {
                while (it.moveToNext()) {
                    tasks.add(cursorToTask(it))
                }
            }
            
            withContext(Dispatchers.Main) {
                taskAdapter?.updateTasks(tasks)
            }
        }
    }

    private fun loadImportantTasks() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 首先检查列是否存在
                val cursor = dbHelper.readableDatabase.rawQuery(
                    "SELECT * FROM ToDoListLocal LIMIT 1", null
                )
                val columnNames = cursor.columnNames.toList()
                cursor.close()

                val hasImportantColumn = columnNames.contains("is_important")
                
                // 根据列是否存在使用不同的查询
                val query = if (hasImportantColumn) {
                    dbHelper.readableDatabase.query(
                        "ToDoListLocal",
                        null,
                        "is_important = ?",
                        arrayOf("1"),
                        null,
                        null,
                        "start_time DESC"
                    )
                } else {
                    // 如果列不存在，返回空结果
                    dbHelper.readableDatabase.query(
                        "ToDoListLocal",
                        null,
                        "1 = 0", // 始终为false的条件
                        null,
                        null,
                        null,
                        "start_time DESC"
                    )
                }

                val tasks = mutableListOf<Task>()
                query.use {
                    while (it.moveToNext()) {
                        tasks.add(cursorToTask(it))
                    }
                }

                withContext(Dispatchers.Main) {
                    taskAdapter?.updateTasks(tasks)
                }
            } catch (e: Exception) {
                Log.e("HomeFragment", "加载重要任务失败", e)
                withContext(Dispatchers.Main) {
                    taskAdapter?.updateTasks(emptyList())
                }
            }
        }
    }

    private fun loadCompletedTasks() {
        lifecycleScope.launch(Dispatchers.IO) {
            val cursor = dbHelper.readableDatabase.query(
                "ToDoListLocal",
                null,
                "is_completed = ?",
                arrayOf("1"),
                null,
                null,
                "start_time DESC"
            )
            
            val tasks = mutableListOf<Task>()
            cursor.use {
                while (it.moveToNext()) {
                    tasks.add(cursorToTask(it))
                }
            }
            
            withContext(Dispatchers.Main) {
                taskAdapter?.updateTasks(tasks)
            }
        }
    }

    private fun cursorToTask(cursor: android.database.Cursor): Task {
        return Task(
            listId = cursor.getString(cursor.getColumnIndexOrThrow("list_id")),
            userId = cursor.getString(cursor.getColumnIndexOrThrow("user_id")),
            startTime = cursor.getString(cursor.getColumnIndexOrThrow("start_time")),
            endTime = cursor.getString(cursor.getColumnIndexOrThrow("end_time")),
            location = cursor.getString(cursor.getColumnIndexOrThrow("location")),
            content = cursor.getString(cursor.getColumnIndexOrThrow("todo_content")),
            syncStatus = cursor.getInt(cursor.getColumnIndexOrThrow("sync_status")),
            lastModified = cursor.getString(cursor.getColumnIndexOrThrow("last_modified"))
        )
    }
} 