package com.example.notiflogger

import android.app.PendingIntent

/**
 * Holds the original PendingIntent for recently-logged notifications so tapping
 * an entry can reopen the exact message. This only survives while the app/service
 * process is alive (PendingIntents can't be persisted to disk), so entries logged
 * before the last process restart fall back to just opening the source app.
 */
object PendingIntentCache {
    private const val MAX_SIZE = 300
    private val map = object : LinkedHashMap<Long, PendingIntent>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.Entry<Long, PendingIntent>?): Boolean {
            return size > MAX_SIZE
        }
    }

    @Synchronized
    fun put(id: Long, pendingIntent: PendingIntent) {
        map[id] = pendingIntent
    }

    @Synchronized
    fun get(id: Long): PendingIntent? = map[id]
}
