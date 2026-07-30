package com.example.notiflogger

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.notiflogger.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var dao: NotificationDao
    private val adapter = NotificationAdapter { entity -> onNotificationClicked(entity) }
    private var currentLiveData: LiveData<List<NotificationEntity>>? = null
    private var searchQuery: String = ""

    private val observer = Observer<List<NotificationEntity>> { flat ->
        adapter.submitList(buildGroupedList(flat, FilterSortState.sortMode))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dao = AppDatabase.getDatabase(applicationContext).notificationDao()

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        binding.buttonGrantAccess.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        binding.buttonSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        binding.searchBox.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s?.toString().orEmpty()
                loadData()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    override fun onResume() {
        super.onResume()
        // Filters/sort may have changed in SettingsActivity
        loadData()
    }

    private fun loadData() {
        currentLiveData?.removeObserver(observer)
        val liveData = dao.getFiltered(FilterSortState.filterApp, FilterSortState.filterDate, searchQuery)
        currentLiveData = liveData
        liveData.observe(this, observer)
    }

    private fun onNotificationClicked(entity: NotificationEntity) {
        val pendingIntent: PendingIntent? = PendingIntentCache.get(entity.id)
        if (pendingIntent != null) {
            try {
                pendingIntent.send()
                return
            } catch (e: PendingIntent.CanceledException) {
                // Fall through to launching the app instead
            }
        }
        val launchIntent = packageManager.getLaunchIntentForPackage(entity.packageName)
        if (launchIntent != null) {
            startActivity(launchIntent)
        } else {
            Toast.makeText(this, "Couldn't open ${entity.appName}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun buildGroupedList(flat: List<NotificationEntity>, sort: SortMode): List<ListItem> {
        val result = mutableListOf<ListItem>()
        val dayKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        when (sort) {
            SortMode.DATE_DESC, SortMode.DATE_ASC -> {
                val groups = flat.groupBy { dayKeyFormat.format(Date(it.timestamp)) }
                var keys = groups.keys.sorted()
                if (sort == SortMode.DATE_DESC) keys = keys.reversed()
                for (key in keys) {
                    val items = groups[key]!!.sortedByDescending { it.timestamp }
                        .let { if (sort == SortMode.DATE_ASC) it.reversed() else it }
                    result.add(ListItem.Header(dateLabel(key), items.size))
                    items.forEach { result.add(ListItem.Row(it)) }
                }
            }
            SortMode.APP_NAME -> {
                val groups = flat.groupBy { it.appName }
                for (appName in groups.keys.sortedBy { it.lowercase(Locale.getDefault()) }) {
                    val items = groups[appName]!!.sortedByDescending { it.timestamp }
                    result.add(ListItem.Header(appName, items.size))
                    items.forEach { result.add(ListItem.Row(it)) }
                }
            }
            SortMode.APP_COUNT -> {
                val groups = flat.groupBy { it.appName }
                val sortedApps = groups.entries.sortedByDescending { it.value.size }
                for (entry in sortedApps) {
                    val items = entry.value.sortedByDescending { it.timestamp }
                    result.add(ListItem.Header(entry.key, items.size))
                    items.forEach { result.add(ListItem.Row(it)) }
                }
            }
        }
        return result
    }

    private fun dateLabel(key: String): String {
        val dayKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = dayKeyFormat.format(Date())
        val cal = Calendar.getInstance()
        cal.add(Calendar.DATE, -1)
        val yesterday = dayKeyFormat.format(cal.time)
        return when (key) {
            today -> "Today"
            yesterday -> "Yesterday"
            else -> {
                val outFmt = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                outFmt.format(dayKeyFormat.parse(key)!!)
            }
        }
    }
}
