package com.minmah.AplikasiTugasHarian

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.minmah.AplikasiTugasHarian.databinding.DialogCopyTasksBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CopyTasksDialogFragment : DialogFragment() {

    // Listener untuk mengirim data kembali ke MainActivity
    interface CopyTasksListener {
        fun onTasksCopied(sourceDate: Calendar, startDate: Calendar, endDate: Calendar)
    }

    private var listener: CopyTasksListener? = null

    private lateinit var binding: DialogCopyTasksBinding
    private var sourceDate: Calendar? = null
    private var startDate: Calendar? = null
    private var endDate: Calendar? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogCopyTasksBinding.inflate(layoutInflater)

        // Set listener dari context (MainActivity)
        if (context is CopyTasksListener) {
            listener = context as CopyTasksListener
        }

        setupDatePickers()

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Salin Tugas Harian")
            .setView(binding.root)
            .setPositiveButton("Salin", null) // Kita override listener-nya nanti
            .setNegativeButton("Batal") { dialog, _ -> dialog.dismiss() }
            .create()

        // Override listener tombol positif untuk validasi
        dialog.setOnShowListener { 
            val positiveButton = dialog.getButton(Dialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                if (validateDates()) {
                    listener?.onTasksCopied(sourceDate!!, startDate!!, endDate!!)
                    dismiss()
                }
            }
        }

        return dialog
    }

    private fun setupDatePickers() {
        binding.textSourceDate.setOnClickListener { showDatePicker(it as TextView) { cal -> sourceDate = cal } }
        binding.textStartDate.setOnClickListener { showDatePicker(it as TextView) { cal -> startDate = cal } }
        binding.textEndDate.setOnClickListener { showDatePicker(it as TextView) { cal -> endDate = cal } }
    }

    private fun showDatePicker(textView: TextView, onDateSet: (Calendar) -> Unit) {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val selectedCalendar = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }
                val sdf = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())
                textView.text = sdf.format(selectedCalendar.time)
                onDateSet(selectedCalendar)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun validateDates(): Boolean {
        if (sourceDate == null || startDate == null || endDate == null) {
            Toast.makeText(context, "Semua tanggal harus diisi", Toast.LENGTH_SHORT).show()
            return false
        }
        if (startDate!!.after(endDate)) {
            Toast.makeText(context, "Tanggal mulai tidak boleh setelah tanggal selesai", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }
}
