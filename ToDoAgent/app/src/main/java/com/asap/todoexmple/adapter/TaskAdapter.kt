package com.asap.todoexmple.adapter

import Task
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageButton
import androidx.recyclerview.widget.RecyclerView
import com.asap.todoexmple.R


class TaskAdapter : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {
    private var tasks: List<Task> = emptyList()
    private var onCompleteClickListener: ((Task, Boolean) -> Unit)? = null
    private var onImportantClickListener: ((Task, Boolean) -> Unit)? = null

    class TaskViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val contentText: TextView = view.findViewById(R.id.tvContent)
        val timeText: TextView = view.findViewById(R.id.tvTime)
        val locationText: TextView = view.findViewById(R.id.tvLocation)
        val checkComplete: CheckBox = view.findViewById(R.id.checkComplete)
        val btnImportant: ImageButton = view.findViewById(R.id.btnImportant)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]
        holder.contentText.text = task.content
        holder.timeText.text = "${task.startTime ?: ""} - ${task.endTime ?: ""}"
        holder.locationText.apply {
            text = task.location ?: ""
            visibility = if (task.location.isNullOrEmpty()) View.GONE else View.VISIBLE
        }
        
        // 设置完成状态
        holder.checkComplete.isChecked = task.isCompleted
        holder.checkComplete.setOnCheckedChangeListener { _, isChecked ->
            onCompleteClickListener?.invoke(task, isChecked)
        }
        
        // 设置重要状态
        holder.btnImportant.isSelected = task.isImportant
        holder.btnImportant.setOnClickListener {
            holder.btnImportant.isSelected = !holder.btnImportant.isSelected
            onImportantClickListener?.invoke(task, holder.btnImportant.isSelected)
        }
    }

    override fun getItemCount() = tasks.size

    fun updateTasks(newTasks: List<Task>) {
        tasks = newTasks
        notifyDataSetChanged()
    }

    fun setOnCompleteClickListener(listener: (Task, Boolean) -> Unit) {
        onCompleteClickListener = listener
    }

    fun setOnImportantClickListener(listener: (Task, Boolean) -> Unit) {
        onImportantClickListener = listener
    }
} 