package com.minmah.AplikasiTugasHarian

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
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
    private lateinit var specialTaskDao: SpecialTaskDao

    private var selectedDate: Date = Date() // Default ke hari ini
    private var selectedTaskFilterType: String = "Tugas Harian" // Default filter

    private var observeJob: Job? = null
    private var calendarAnimator: ObjectAnimator? = null
    private var isCalendarAnimating = false // Add this flag


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val database = TaskDatabase.getDatabase(applicationContext)
        taskDao = database.taskDao()
        specialTaskDao = database.specialTaskDao()

        setupRecyclerViews()
        setupSpinners()
        setupActionButtons()
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
            onTaskChecked = { task -> updateTaskStatus(task) },
            onDeleteClicked = { task -> deleteTask(task) }
        )
        binding.recyclerViewTasks.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = taskAdapter

            // Tambahkan scroll listener untuk menyembunyikan/menampilkan kalender
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)

                    // Scroll ke bawah → sembunyikan kalender
                    if (dy > 0 && binding.calendarContainer.visibility == View.VISIBLE) {
                        hideCalendarContainer()
                    }

                    // Hanya kalau user tarik ke bawah saat sudah di posisi atas
                    if (dy < -10 && !recyclerView.canScrollVertically(-1) &&
                        binding.calendarContainer.visibility == View.GONE) {
                        showCalendarContainer()
                    }
                }
            })

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

        // --- Spinner Filter Jenis Tugas ---
        val taskTypes = listOf("Tugas Harian", "Tugas Spesial")
        val taskTypeAdapter = ArrayAdapter(this, R.layout.custom_spinner_item, taskTypes)
        taskTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerTaskType.adapter = taskTypeAdapter
        binding.spinnerTaskType.setSelection(0) // Default: Tugas Harian

        // --- Listener untuk Spinner Bulan & Tahun ---
        val dateSpinnerListener = object : AdapterView.OnItemSelectedListener {
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

        binding.spinnerYear.onItemSelectedListener = dateSpinnerListener
        binding.spinnerMonth.onItemSelectedListener = dateSpinnerListener

        // --- Listener untuk Spinner Filter Jenis Tugas ---
        binding.spinnerTaskType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedTaskFilterType = taskTypes[position]
                observeTasksForDay()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

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

            val taskDate = normalizeDateToStartOfDay(selectedDate)

            // Tambahkan tugas berdasarkan pilihan RadioButton
            when (binding.radioGroupTaskType.checkedRadioButtonId) {
                R.id.radioDaily -> {
                    val newTask = Task(date = taskDate, description = description)
                    lifecycleScope.launch { taskDao.insert(newTask) }
                }
                R.id.radioSpecial -> {
                    val newSpecialTask = SpecialTask(date = taskDate, description = description)
                    lifecycleScope.launch { specialTaskDao.insert(newSpecialTask) }
                }
            }
            binding.editTextTask.text.clear()
            binding.radioDaily.isChecked = true // Reset ke Tugas Harian
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
            when (selectedTaskFilterType) {
                "Tugas Harian" -> {
                    taskDao.getTasksByDate(normalizedDate).collectLatest { tasks ->
                        taskAdapter.submitList(tasks)
                        val completedTasks = tasks.count { it.isCompleted }
                        val totalTasks = tasks.size
                        binding.textViewTaskSummary.text = "$completedTasks/$totalTasks"
                    }
                }
                "Tugas Spesial" -> {
                    specialTaskDao.getTasksByDate(normalizedDate).collectLatest { specialTasks ->
                        // Konversi SpecialTask ke Task untuk adapter
                        val tasks = specialTasks.map { Task(it.id, it.date, it.description, it.isCompleted) }
                        taskAdapter.submitList(tasks)
                        val completedTasks = tasks.count { it.isCompleted }
                        val totalTasks = tasks.size
                        binding.textViewTaskSummary.text = "$completedTasks/$totalTasks"
                    }
                }
            }
        }
    }

    // Callback dari CopyTasksDialogFragment
    override fun onTasksCopied(sourceDate: Calendar, startDate: Calendar, endDate: Calendar) {
        lifecycleScope.launch {
            val normalizedSourceMillis = normalizeDateToStartOfDay(sourceDate.time)

            val sourceTasksGeneric: List<Any> = when (selectedTaskFilterType) {
                "Tugas Harian" -> taskDao.getTasksByDateOnce(normalizedSourceMillis)
                "Tugas Spesial" -> specialTaskDao.getTasksByDateOnce(normalizedSourceMillis)
                else -> emptyList() // Should not happen with current filter types
            }

            if (sourceTasksGeneric.isEmpty()) {
                Toast.makeText(this@MainActivity, "Tidak ada tugas untuk disalin dari tanggal sumber.", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val newTasks = mutableListOf<Task>()
            val newSpecialTasks = mutableListOf<SpecialTask>()
            val loopCalendar = startDate.clone() as Calendar

            val endMillis = normalizeDateToStartOfDay(endDate.time)

            while (normalizeDateToStartOfDay(loopCalendar.time) <= endMillis) {
                val targetDateMillis = normalizeDateToStartOfDay(loopCalendar.time)
                for (sourceTask in sourceTasksGeneric) {
                    when (sourceTask) {
                        is Task -> newTasks.add(sourceTask.copy(id = 0, date = targetDateMillis))
                        is SpecialTask -> newSpecialTasks.add(sourceTask.copy(id = 0, date = targetDateMillis))
                    }
                }
                loopCalendar.add(Calendar.DAY_OF_YEAR, 1)
            }

            if (newTasks.isNotEmpty()) {
                taskDao.insertAll(newTasks)
                val dayCount = newTasks.size / (sourceTasksGeneric.filterIsInstance<Task>().size.takeIf { it > 0 } ?: 1)
                Toast.makeText(this@MainActivity, "${sourceTasksGeneric.filterIsInstance<Task>().size} tugas harian berhasil disalin ke $dayCount hari.", Toast.LENGTH_LONG).show()
            }
            if (newSpecialTasks.isNotEmpty()) {
                specialTaskDao.insertAll(newSpecialTasks)
                val dayCount = newSpecialTasks.size / (sourceTasksGeneric.filterIsInstance<SpecialTask>().size.takeIf { it > 0 } ?: 1)
                Toast.makeText(this@MainActivity, "${sourceTasksGeneric.filterIsInstance<SpecialTask>().size} tugas spesial berhasil disalin ke $dayCount hari.", Toast.LENGTH_LONG).show()
            }

            observeTasksForDay()
        }
    }

    // --- FUNGSI CRUD & BANTU --- //

    private fun updateTaskStatus(task: Task) {
        lifecycleScope.launch {
            // Perbarui di DAO yang sesuai
            when (selectedTaskFilterType) {
                "Tugas Harian" -> taskDao.update(task)
                "Tugas Spesial" -> {
                    // Konversi Task kembali ke SpecialTask untuk update
                    val specialTask = SpecialTask(task.id, task.date, task.description, task.isCompleted)
                    specialTaskDao.update(specialTask)
                }
            }
        }
    }

    private fun deleteTask(task: Task) {
        lifecycleScope.launch {
            // Hapus dari DAO yang sesuai
            when (selectedTaskFilterType) {
                "Tugas Harian" -> taskDao.delete(task.id)
                "Tugas Spesial" -> specialTaskDao.delete(task.id)
            }
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


    // --- FUNGSI ANIMASI UI --- //
    private fun hideCalendarContainer() {
        calendarAnimator?.cancel() // Batalkan animasi yang sedang berjalan
        if (binding.calendarContainer.visibility == View.GONE) return

        calendarAnimator = ObjectAnimator.ofFloat(binding.calendarContainer, "alpha", 1f, 0f).apply {
            duration = 300
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    binding.calendarContainer.visibility = View.GONE
                    calendarAnimator = null // Hapus referensi animator
                }
            })
            start()
        }
    }

    private fun showCalendarContainer() {
        if (isCalendarAnimating || binding.calendarContainer.visibility == View.VISIBLE) return
        isCalendarAnimating = true

        calendarAnimator?.cancel()

        binding.calendarContainer.alpha = 0f
        binding.calendarContainer.visibility = View.VISIBLE

        binding.calendarContainer.animate()
            .alpha(1f)
            .setDuration(300)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    isCalendarAnimating = false
                    calendarAnimator = null
                }

                override fun onAnimationCancel(animation: Animator) {
                    isCalendarAnimating = false
                }
            })
            .start()
    }

}