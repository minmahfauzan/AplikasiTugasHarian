package com.minmah.AplikasiTugasHarian

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.minmah.AplikasiTugasHarian.databinding.ItemTaskBinding

class TaskAdapter(
    private val onTaskChecked: (Task) -> Unit,
    private val onDeleteClicked: (Task) -> Unit
) : ListAdapter<Task, TaskAdapter.TaskViewHolder>(TaskDiffCallback()) {

    inner class TaskViewHolder(private val binding: ItemTaskBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(task: Task) {
            binding.checkBoxTask.text = task.description
            binding.checkBoxTask.isChecked = task.isCompleted

            applyStrikeThrough(task.isCompleted)

            // Set listener ke null dulu untuk mencegah trigger saat re-binding
            binding.checkBoxTask.setOnCheckedChangeListener(null)
            binding.checkBoxTask.isChecked = task.isCompleted

            // Set listener yang sebenarnya
            binding.checkBoxTask.setOnCheckedChangeListener { _, isChecked ->
                onTaskChecked(task.copy(isCompleted = isChecked))
            }

            binding.buttonDelete.setOnClickListener {
                onDeleteClicked(task)
            }
        }

        private fun applyStrikeThrough(isCompleted: Boolean) {
            if (isCompleted) {
                binding.checkBoxTask.paintFlags = binding.checkBoxTask.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                binding.checkBoxTask.paintFlags = binding.checkBoxTask.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

// Kelas DiffUtil untuk menghitung perbedaan antar list
class TaskDiffCallback : DiffUtil.ItemCallback<Task>() {
    override fun areItemsTheSame(oldItem: Task, newItem: Task): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Task, newItem: Task): Boolean {
        return oldItem == newItem
    }
}
