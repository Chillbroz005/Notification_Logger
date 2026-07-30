package com.example.notiflogger

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class MyNotificationListenerService : NotificationListenerService() {

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        // Skip our own notifications (avoids self-logging loops)
        if (sbn.packageName == applicationContext.packageName) return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence("android.title")?.toString().orEmpty()
        val text = extras.getCharSequence("android.text")?.toString().orEmpty()

        // Skip empty/ongoing summary notifications with no content
        if (title.isEmpty() && text.isEmpty()) return

        val appName = getAppNameFromPackage(sbn.packageName)
        val contentIntent = sbn.notification.contentIntent
        val imageBitmap = extractImage(sbn)

        scope.launch {
            val imagePath = imageBitmap?.let { saveBitmapToFile(it) }

            val entity = NotificationEntity(
                packageName = sbn.packageName,
                appName = appName,
                title = title,
                text = text,
                timestamp = sbn.postTime,
                imagePath = imagePath
            )

            val id = AppDatabase.getDatabase(applicationContext).notificationDao().insert(entity)
            if (contentIntent != null) {
                PendingIntentCache.put(id, contentIntent)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // No-op: we keep the historical log even after the notification is dismissed
    }

    private fun getAppNameFromPackage(packageName: String): String {
        return try {
            val pm = applicationContext.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }
    }

    /**
     * Tries to pull the image shown inside the notification (BigPictureStyle photo,
     * falling back to the large icon/avatar if no full picture is present).
     */
    private fun extractImage(sbn: StatusBarNotification): Bitmap? {
        val extras = sbn.notification.extras

        // Older BigPictureStyle notifications store a raw Bitmap here
        (extras.get("android.picture") as? Bitmap)?.let { return it }

        // Android 12+ BigPictureStyle notifications store an Icon instead
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (extras.get("android.picture.icon") as? Icon)?.let { icon ->
                iconToBitmap(icon)?.let { return it }
            }
        }

        // Fallback: the small avatar/large icon shown next to the notification
        val largeIcon: Icon? = sbn.notification.getLargeIcon()
        if (largeIcon != null) {
            iconToBitmap(largeIcon)?.let { return it }
        }

        return null
    }

    private fun iconToBitmap(icon: Icon): Bitmap? {
        return try {
            val drawable = icon.loadDrawable(applicationContext) ?: return null
            val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 128
            val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 128
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    private fun saveBitmapToFile(bitmap: Bitmap): String? {
        return try {
            val dir = File(applicationContext.filesDir, "notification_images")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "${UUID.randomUUID()}.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }
}
