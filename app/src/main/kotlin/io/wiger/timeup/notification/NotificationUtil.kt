package io.wiger.timeup.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.wiger.timeup.R
import io.wiger.timeup.core.AppSettings

object NotificationUtil {

    const val CHANNEL_ID = "Time_Up_Channel"

    @JvmStatic
    fun hasPostNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    @JvmStatic
    fun createNotificationChannel(context: Context) {
        val name = context.getString(R.string.notify_channel_name)
        val description = context.getString(R.string.notify_channel_desc)
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            this.description = description
        }

        NotificationManagerCompat.from(context).createNotificationChannel(channel)
    }

    @JvmStatic
    fun baseNotification(context: Context): NotificationCompat.Builder =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.alarm)

    /**
     * Android 16+ only. Returns false on older versions.
     */
    @JvmStatic
    fun canPostPromotedNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.BAKLAVA) return false
        return runCatching {
            ContextCompat.getSystemService(context, NotificationManager::class.java)
                ?.canPostPromotedNotifications() == true
        }.getOrDefault(false)
    }

    /**
     * Effective state for requesting promoted ongoing notifications. The user preference only
     * matters when notifications are allowed and the system has granted live updates.
     */
    @JvmStatic
    fun isLiveNotificationEnabled(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.BAKLAVA) return false
        if (!hasPostNotificationPermission(context)) return false
        if (!canPostPromotedNotifications(context)) return false
        return AppSettings.liveNotificationEnabled.value
    }

    @JvmStatic
    fun setLiveNotificationEnabled(context: Context, enabled: Boolean) {
        AppSettings.setLiveNotificationEnabled(enabled)
    }
}
