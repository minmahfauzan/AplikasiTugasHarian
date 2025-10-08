package com.minmah.AplikasiTugasHarian

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.minmah.AplikasiTugasHarian.databinding.ItemTaskBinding // Menggunakan View Binding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TaskAdapter(
    private var tasks: List<Task>,
    private val onTaskChecked: (Task) -> Unit, // Fungsi saat CheckBox di klik
    private val onDeleteClicked: (Task) -> Unit // Fungsi saat tombol Hapus di klik
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    // --- ViewHolder: Menghubungkan layout item_task.xml dengan kode ---
    inner class TaskViewHolder(private val binding: ItemTaskBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(task: Task) {
            // 1. Tampilkan Deskripsi dan Status Selesai
            binding.checkBoxTask.text = task.description
            binding.checkBoxTask.isChecked = task.isCompleted

            // 2. Beri efek coret jika tugas sudah selesai
            applyStrikeThrough(task.isCompleted)

            // 3. Listener untuk CheckBox
            binding.checkBoxTask.setOnCheckedChangeListener { _, isChecked ->
                // Panggil fungsi di MainActivity saat status berubah
                onTaskChecked(task.copy(isCompleted = isChecked))
            }

            // 4. Listener untuk Tombol Hapus
            binding.buttonDelete.setOnClickListener {
                onDeleteClicked(task)
            }
        }

        // Fungsi bantu untuk mencoret teks
        private fun applyStrikeThrough(isCompleted: Boolean) {
            if (isCompleted) {
                binding.checkBoxTask.paintFlags = binding.checkBoxTask.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                binding.checkBoxTask.paintFlags = binding.checkBoxTask.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }
        }
    }
    // --- Akhir ViewHolder ---

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        // Inflate layout item_task.xml
        val binding = ItemTaskBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(tasks[position])
    }

    override fun getItemCount(): Int = tasks.size

    // Fungsi untuk memperbarui daftar tugas saat ada perubahan data
    fun updateTasks(newTasks: List<Task>) {
        tasks = newTasks
        notifyDataSetChanged() // Memberi tahu RecyclerView bahwa data berubah
    }
}