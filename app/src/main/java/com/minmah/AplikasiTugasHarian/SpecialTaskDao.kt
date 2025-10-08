package com.minmah.AplikasiTugasHarian

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SpecialTaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: SpecialTask)

    @Query("SELECT * FROM special_task_table WHERE date = :selectedDate ORDER BY id ASC")
    fun getTasksByDate(selectedDate: Long): Flow<List<SpecialTask>>

    @Update
    suspend fun update(task: SpecialTask)

    @Query("DELETE FROM special_task_table WHERE id = :taskId")
    suspend fun delete(taskId: Int)

    @Query("SELECT * FROM special_task_table WHERE date = :selectedDate")
    suspend fun getTasksByDateOnce(selectedDate: Long): List<SpecialTask>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<SpecialTask>)
}