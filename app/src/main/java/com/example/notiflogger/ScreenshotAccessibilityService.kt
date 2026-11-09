package com.example.notiflogger

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class ScreenshotAccessibilityService : AccessibilityService() {

    companion object {
        var instance: ScreenshotAccessibilityService? = null
            private set

        fun isRunning(): Boolean = instance != null

        fun captureScreenshot(context: Context, onSaved: ((String?) -> Unit)? = null): Boolean {
            val service = instance ?: return false
            return service.performCapture(context, onSaved)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No-op: only needed for global action and screenshot capability
    }

    override fun onInterrupt() {
        // No-op
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
    }

    fun performCapture(context: Context, onSaved: ((String?) -> Unit)? = null): Boolean {
        var triggered = false

        // Android 9+ (API 28): triggers the native system screenshot using the device format
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            triggered = performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
        }

        // Android 11+ (API 30): capture the bitmap in memory to attach to the notification entry
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                takeScreenshot(
                    Display.DEFAULT_DISPLAY,
                    applicationContext.mainExecutor,
                    object : TakeScreenshotCallback {
                        override fun onSuccess(screenshot: ScreenshotResult) {
                            try {
                                val hwBuffer = screenshot.hardwareBuffer
                                val colorSpace = screenshot.colorSpace
                                val bitmap = Bitmap.wrapHardwareBuffer(hwBuffer, colorSpace)
                                hwBuffer.close()

                                if (bitmap != null) {
                                    val copy = bitmap.copy(Bitmap.Config.ARGB_8888, false)
                                    val path = saveScreenshotToFile(context, copy)
                                    onSaved?.invoke(path)
                                    return
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            onSaved?.invoke(null)
                        }

                        override fun onFailure(errorCode: Int) {
                            onSaved?.invoke(null)
                        }
                    }
                )
            } catch (e: Exception) {
                e.printStackTrace()
                onSaved?.invoke(null)
            }
        } else {
            onSaved?.invoke(null)
        }

        return triggered
    }

    private fun saveScreenshotToFile(context: Context, bitmap: Bitmap): String? {
        return try {
            val dir = File(context.filesDir, "notification_images")
            if (!dir.exists()) dir.mkdirs()
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(dir, "Screenshot_${timeStamp}_${UUID.randomUUID().toString().take(6)}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }
}
