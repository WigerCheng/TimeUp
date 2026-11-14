package io.wiger.timeup

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import io.wiger.timeup.core.AppSettings
import io.wiger.timeup.notification.NotificationUtil

@HiltAndroidApp
class TimeUpApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        AppSettings.init(this)
        NotificationUtil.createNotificationChannel(this)
    }
}
