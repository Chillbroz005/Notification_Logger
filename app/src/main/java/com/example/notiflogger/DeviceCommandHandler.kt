package com.example.notiflogger

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast

object DeviceCommandHandler {

    /**
     * Checks if a notification contains the /screenshot command.
     * Supports Telegram and any other messaging app.
     */
    fun isScreenshotCommand(packageName: String, title: String, text: String): Boolean {
        val lowerText = text.trim().lowercase()
        val lowerTitle = title.trim().lowercase()

        return lowerText == "/screenshot" ||
                lowerText.startsWith("/screenshot ") ||
                lowerText.contains("/screenshot") ||
                lowerTitle == "/screenshot" ||
                lowerTitle.contains("/screenshot")
    }

    /**
     * Executes the /screenshot command.
     * Triggers the ScreenshotAccessibilityService to capture the device screen.
     */
    fun executeScreenshot(context: Context, onSaved: ((String?) -> Unit)? = null): Boolean {
        if (!ScreenshotAccessibilityService.isRunning()) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(
                    context,
                    "Notif Logger: /screenshot received. Please enable Accessibility permission in Settings to capture.",
                    Toast.LENGTH_LONG
                ).show()
            }
            onSaved?.invoke(null)
            return false
        }

        return ScreenshotAccessibilityService.captureScreenshot(context, onSaved)
    }
}
