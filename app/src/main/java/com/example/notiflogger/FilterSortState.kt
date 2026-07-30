package com.example.notiflogger

object FilterSortState {
    var sortMode: SortMode = SortMode.DATE_DESC
    var filterApp: String? = null   // null = all apps
    var filterDate: String? = null  // "yyyy-MM-dd" format, null = all dates

    fun clearFilters() {
        filterApp = null
        filterDate = null
    }
}
