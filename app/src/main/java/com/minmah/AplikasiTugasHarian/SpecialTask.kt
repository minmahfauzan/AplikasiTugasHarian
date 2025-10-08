package com.minmah.AplikasiTugasHarian

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "special_task_table")
data class SpecialTask(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val date: Long,
    val description: String,
    var isCompleted: Boolean = false
)