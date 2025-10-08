package com.minmah.AplikasiTugasHarian

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.minmah.AplikasiTugasHarian.databinding.CalendarDayItemBinding
import java.util.Calendar
import java.util.Date

// Data class untuk merepresentasikan satu hari di kalender
data class CalendarDay(val date: Date, var isSelected: Boolean = false)

class CalendarAdapter(
    private val onDayClicked: (Date) -> Unit
) : RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder>() {

    private val days = mutableListOf<CalendarDay?>()

    inner class CalendarViewHolder(private val binding: CalendarDayItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(calendarDay: CalendarDay?) {
            if (calendarDay == null) {
                binding.textViewDay.text = ""
                binding.root.isClickable = false
                return
            }

            val cal = Calendar.getInstance().apply { time = calendarDay.date }
            val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
            binding.textViewDay.text = dayOfMonth.toString()
            binding.root.isClickable = true

            // Atur tampilan visual berdasarkan status hari
            when {
                calendarDay.isSelected -> {
                    binding.textViewDay.setTextColor(Color.WHITE)
                    binding.root.setBackgroundResource(R.drawable.bg_selected_day)
                }
                isToday(calendarDay.date) -> {
                    binding.textViewDay.setTextColor(ContextCompat.getColor(itemView.context, R.color.purple_500))
                    binding.root.setBackgroundColor(Color.TRANSPARENT)
                }
                else -> {
                    // Hari normal
                    binding.textViewDay.setTextColor(Color.LTGRAY)
                    binding.root.setBackgroundColor(Color.TRANSPARENT)
                }
            }

            // Listener klik sekarang hanya memanggil lambda
            binding.root.setOnClickListener {
                onDayClicked(calendarDay.date)
            }
        }

        private fun isToday(date: Date): Boolean {
            val today = Calendar.getInstance()
            val comparison = Calendar.getInstance().apply { time = date }
            return today.get(Calendar.YEAR) == comparison.get(Calendar.YEAR) &&
                   today.get(Calendar.DAY_OF_YEAR) == comparison.get(Calendar.DAY_OF_YEAR)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalendarViewHolder {
        val binding = CalendarDayItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CalendarViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CalendarViewHolder, position: Int) {
        holder.bind(days[position])
    }

    override fun getItemCount(): Int = days.size

    fun setDays(year: Int, month: Int, selectedDate: Date) {
        days.clear()
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
        }

        val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1
        for (i in 0 until firstDayOfWeek) {
            days.add(null)
        }

        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        for (i in 1..daysInMonth) {
            calendar.set(Calendar.DAY_OF_MONTH, i)
            val dayDate = calendar.time
            val isSelectedDay = isSameDay(dayDate, selectedDate)
            days.add(CalendarDay(dayDate, isSelectedDay))
        }

        notifyDataSetChanged()
    }

    private fun isSameDay(date1: Date, date2: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = date1 }
        val cal2 = Calendar.getInstance().apply { time = date2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}