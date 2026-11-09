package com.example.notiflogger

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface NotificationDao {

    @Insert
    suspend fun insert(notification: NotificationEntity): Long

    @Query(
        "SELECT * FROM notifications WHERE " +
        "(:appFilter IS NULL OR appName = :appFilter) AND " +
        "(:dateFilter IS NULL OR date(timestamp / 1000, 'unixepoch', 'localtime') = :dateFilter) AND " +
        "(:query = '' OR appName LIKE '%' || :query || '%' OR title LIKE '%' || :query || '%' OR text LIKE '%' || :query || '%') " +
        "ORDER BY timestamp DESC"
    )
    fun getFiltered(appFilter: String?, dateFilter: String?, query: String): LiveData<List<NotificationEntity>>

    @Query("SELECT DISTINCT appName FROM notifications ORDER BY appName ASC")
    fun getDistinctApps(): LiveData<List<String>>

    @Query("SELECT * FROM notifications ORDER BY timestamp ASC")
    suspend fun getAllForExport(): List<NotificationEntity>

    @Query("DELETE FROM notifications")
    suspend fun clearAll()

    @Query("UPDATE notifications SET imagePath = :imagePath WHERE id = :id")
    suspend fun updateImagePath(id: Long, imagePath: String)
}
