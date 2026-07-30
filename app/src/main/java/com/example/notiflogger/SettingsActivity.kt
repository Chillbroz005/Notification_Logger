package com.example.notiflogger

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.example.notiflogger.databinding.ActivitySettingsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private var pickedDate: String? = null // "yyyy-MM-dd"
    private var appList: List<String> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Pre-select current sort mode
        when (FilterSortState.sortMode) {
            SortMode.DATE_DESC -> binding.radioLatestFirst.isChecked = true
            SortMode.DATE_ASC -> binding.radioOldestFirst.isChecked = true
            SortMode.APP_NAME -> binding.radioAppName.isChecked = true
            SortMode.APP_COUNT -> binding.radioAppCount.isChecked = true
        }

        pickedDate = FilterSortState.filterDate
        updateDateLabel()

        val dao = AppDatabase.getDatabase(applicationContext).notificationDao()
        dao.getDistinctApps().observe(this) { apps ->
            appList = listOf(getString(R.string.all_apps)) + apps
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, appList)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerApp.adapter = adapter
            val selected = FilterSortState.filterApp
            if (selected != null) {
                val index = appList.indexOf(selected)
                if (index >= 0) binding.spinnerApp.setSelection(index)
            }
        }

        binding.buttonPickDate.setOnClickListener { showDatePicker() }

        binding.buttonClearDate.setOnClickListener {
            pickedDate = null
            updateDateLabel()
        }

        binding.buttonClearAllFilters.setOnClickListener {
            FilterSortState.clearFilters()
            pickedDate = null
            binding.spinnerApp.setSelection(0)
            updateDateLabel()
            Toast.makeText(this, "Filters cleared", Toast.LENGTH_SHORT).show()
        }

        binding.buttonApply.setOnClickListener {
            FilterSortState.sortMode = when (binding.radioGroupSort.checkedRadioButtonId) {
                R.id.radioOldestFirst -> SortMode.DATE_ASC
                R.id.radioAppName -> SortMode.APP_NAME
                R.id.radioAppCount -> SortMode.APP_COUNT
                else -> SortMode.DATE_DESC
            }
            val selectedApp = binding.spinnerApp.selectedItem as? String
            FilterSortState.filterApp = if (selectedApp == null || selectedApp == getString(R.string.all_apps)) null else selectedApp
            FilterSortState.filterDate = pickedDate
            finish()
        }

        binding.buttonExport.setOnClickListener { exportToTextFile() }
    }

    private fun showDatePicker() {
        val cal = Calendar.getInstance()
        pickedDate?.let {
            val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(it)
            if (parsed != null) cal.time = parsed
        }
        DatePickerDialog(
            this,
            { _, year, month, day ->
                val cal2 = Calendar.getInstance()
                cal2.set(year, month, day)
                pickedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal2.time)
                updateDateLabel()
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateDateLabel() {
        binding.textSelectedDate.text = pickedDate ?: getString(R.string.no_date_filter)
    }

    private fun exportToTextFile() {
        val dao = AppDatabase.getDatabase(applicationContext).notificationDao()
        lifecycleScope.launch {
            val all = withContext(Dispatchers.IO) { dao.getAllForExport() }
            if (all.isEmpty()) {
                Toast.makeText(this@SettingsActivity, "No notifications to export", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val fmt = SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault())
            val sb = StringBuilder()
            sb.append("Notification History Export\n")
            sb.append("Generated: ${fmt.format(Date())}\n")
            sb.append("Total: ${all.size}\n")
            sb.append("--------------------------------------\n\n")
            for (n in all) {
                sb.append("[${fmt.format(Date(n.timestamp))}] ${n.appName}\n")
                if (n.title.isNotEmpty()) sb.append("Title: ${n.title}\n")
                if (n.text.isNotEmpty()) sb.append("Message: ${n.text}\n")
                if (n.imagePath != null) sb.append("Image: ${n.imagePath}\n")
                sb.append("\n")
            }

            val exportDir = File(getExternalFilesDir(null), "exports")
            if (!exportDir.exists()) exportDir.mkdirs()
            val fileName = "notifications_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.txt"
            val file = File(exportDir, fileName)

            withContext(Dispatchers.IO) {
                file.writeText(sb.toString())
            }

            Toast.makeText(this@SettingsActivity, "Saved to ${file.absolutePath}", Toast.LENGTH_LONG).show()

            val uri = FileProvider.getUriForFile(
                this@SettingsActivity,
                "${packageName}.fileprovider",
                file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "Save or share export"))
        }
    }
}
