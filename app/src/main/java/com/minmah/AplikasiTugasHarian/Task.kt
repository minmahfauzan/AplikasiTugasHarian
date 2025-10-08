package com.minmah.AplikasiTugasHarian

import androidx.room.Entity
import androidx.room.PrimaryKey

// Menentukan tabel database
@Entity(tableName = "task_table")
data class Task(
    // Kunci utama (Primary Key) yang akan dibuat otomatis
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    // Tanggal dalam format Long (waktu Unix), mudah untuk disimpan & dicari
    val date: Long,

    val description: String,

    var isCompleted: Boolean = false // Default tugas belum selesai
)