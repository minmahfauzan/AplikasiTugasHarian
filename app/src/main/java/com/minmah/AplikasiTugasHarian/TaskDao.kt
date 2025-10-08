package com.minmah.AplikasiTugasHarian

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    // Menambah/memperbarui tugas. Jika terjadi konflik, ganti data lama.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: Task)

    // Mengambil semua tugas untuk tanggal tertentu, diurutkan berdasarkan status
    @Query("SELECT * FROM task_table WHERE date = :selectedDate ORDER BY isCompleted ASC")
    fun getTasksByDate(selectedDate: Long): Flow<List<Task>>

    // Mengambil semua tugas untuk rentang tanggal (bulan) tertentu, diurutkan berdasarkan tanggal & status
    @Query("SELECT * FROM task_table WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC, isCompleted ASC")
    fun getTasksByMonth(startDate: Long, endDate: Long): Flow<List<Task>>

    // Memperbarui status selesai dari sebuah tugas
    @Update
    suspend fun update(task: Task)

    // Menghapus tugas
    @Query("DELETE FROM task_table WHERE id = :taskId")
    suspend fun delete(taskId: Int)

    // Mengambil tugas satu kali (bukan Flow) untuk tanggal tertentu
    @Query("SELECT * FROM task_table WHERE date = :selectedDate")
    suspend fun getTasksByDateOnce(selectedDate: Long): List<Task>

    // Menambah beberapa tugas sekaligus
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<Task>)
}