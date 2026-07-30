package com.example.notiflogger

sealed class ListItem {
    data class Header(val label: String, val count: Int) : ListItem()
    data class Row(val entity: NotificationEntity) : ListItem()
}
