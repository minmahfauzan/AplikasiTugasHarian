package com.minmah.AplikasiTugasHarian

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.minmah.AplikasiTugasHarian.databinding.ActivityMainBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

class MainActivity : AppCompatActivity(), CopyTasksDialogFragment.CopyTasksListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var calendarAdapter: CalendarAdapter
    private lateinit var taskDao: TaskDao

    private var selectedDate: Date = Date() // Default ke hari ini

    private var observeJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val database = TaskDatabase.getDatabase(applicationContext)
        taskDao = database.taskDao()

        setupRecyclerViews()
        setupSpinners()
        setupActionButtons() // Menggantikan setupAddButtonListener
    }

    // --- FUNGSI SETUP --- //

    private fun setupRecyclerViews() {
        calendarAdapter = CalendarAdapter { date ->
            selectedDate = date
            updateCalendarGrid()
        }
        binding.calendarRecyclerView.apply {
            layoutManager = GridLayoutManager(this@MainActivity, 7)
            adapter = calendarAdapter
        }

        taskAdapter = TaskAdapter(
            tasks = emptyList(),
            onTaskChecked = { task -> updateTaskStatus(task) },
            onDeleteClicked = { task -> deleteTask(task) }
        )
        binding.recyclerViewTasks.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = taskAdapter
        }
    }

    private fun setupSpinners() {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)

        val years = (currentYear - 5..currentYear + 5).map { it.toString() }
        val yearAdapter = ArrayAdapter(this, R.layout.custom_spinner_item, years)
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerYear.adapter = yearAdapter
        binding.spinnerYear.setSelection(years.indexOf(currentYear.toString()))

        val months = (0..11).map {
            calendar.set(Calendar.MONTH, it)
            calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, resources.configuration.locale)
        }
        val monthAdapter = ArrayAdapter(this, R.layout.custom_spinner_item, months)
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerMonth.adapter = monthAdapter
        binding.spinnerMonth.setSelection(currentMonth)

        val spinnerListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val year = binding.spinnerYear.selectedItem.toString().toInt()
                val month = binding.spinnerMonth.selectedItemPosition

                val today = Calendar.getInstance()
                if (year != today.get(Calendar.YEAR) || month != today.get(Calendar.MONTH)) {
                    selectedDate = Calendar.getInstance().apply {
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, month)
                        set(Calendar.DAY_OF_MONTH, 1)
                    }.time
                } else {
                    selectedDate = today.time
                }
                updateCalendarGrid()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.spinnerYear.onItemSelectedListener = spinnerListener
        binding.spinnerMonth.onItemSelectedListener = spinnerListener
        
        updateCalendarGrid()
    }

    private fun setupActionButtons() {
        // Listener untuk tombol Tambah
        binding.buttonAdd.setOnClickListener {
            val description = binding.editTextTask.text.toString().trim()
            if (description.isEmpty()) {
                Toast.makeText(this, "Tugas tidak boleh kosong", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val newTask = Task(
                date = normalizeDateToStartOfDay(selectedDate),
                description = description
            )

            lifecycleScope.launch {
                taskDao.insert(newTask)
                binding.editTextTask.text.clear()
            }
        }

        // Listener untuk tombol Salin
        binding.buttonCopy.setOnClickListener {
            CopyTasksDialogFragment().show(supportFragmentManager, "CopyTasksDialog")
        }
    }

    // --- FUNGSI LOGIKA INTI --- //

    private fun updateCalendarGrid() {
        val year = binding.spinnerYear.selectedItem.toString().toInt()
        val month = binding.spinnerMonth.selectedItemPosition
        calendarAdapter.setDays(year, month, selectedDate)
        observeTasksForDay()
    }

    private fun observeTasksForDay() {
        observeJob?.cancel()
        val normalizedDate = normalizeDateToStartOfDay(selectedDate)
        observeJob = lifecycleScope.launch {
            taskDao.getTasksByDate(normalizedDate).collectLatest { tasks ->
                taskAdapter.updateTasks(tasks)
            }
        }
    }

    // Callback dari CopyTasksDialogFragment
    override fun onTasksCopied(sourceDate: Calendar, startDate: Calendar, endDate: Calendar) {
        lifecycleScope.launch {
            val normalizedSourceMillis = normalizeDateToStartOfDay(sourceDate.time)
            val sourceTasks = taskDao.getTasksByDateOnce(normalizedSourceMillis)

            if (sourceTasks.isEmpty()) {
                Toast.makeText(this@MainActivity, "Tidak ada tugas untuk disalin dari tanggal sumber.", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val newTasks = mutableListOf<Task>()
            val loopCalendar = startDate.clone() as Calendar

            // Normalize end date to avoid time-of-day issues in the loop
            val endMillis = normalizeDateToStartOfDay(endDate.time)

            while (normalizeDateToStartOfDay(loopCalendar.time) <= endMillis) {
                val targetDateMillis = normalizeDateToStartOfDay(loopCalendar.time)
                for (sourceTask in sourceTasks) {
                    newTasks.add(sourceTask.copy(id = 0, date = targetDateMillis))
                }
                loopCalendar.add(Calendar.DAY_OF_YEAR, 1)
            }

            if (newTasks.isNotEmpty()) {
                taskDao.insertAll(newTasks)
                val dayCount = newTasks.size / sourceTasks.size
                Toast.makeText(this@MainActivity, "${sourceTasks.size} tugas berhasil disalin ke $dayCount hari.", Toast.LENGTH_LONG).show()
            }
            
            // Refresh the view to show potential changes
            observeTasksForDay()
        }
    }

    // --- FUNGSI CRUD & BANTU --- //

    private fun updateTaskStatus(task: Task) {
        lifecycleScope.launch { taskDao.update(task) }
    }

    private fun deleteTask(task: Task) {
        lifecycleScope.launch {
            taskDao.delete(task.id)
            Toast.makeText(this@MainActivity, "Tugas dihapus!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun normalizeDateToStartOfDay(date: Date): Long {
        return Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}