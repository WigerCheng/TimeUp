package io.wiger.timeup.data.model

import android.os.Parcelable
import io.wiger.timeup.data.local.entity.AppInfoEntity
import kotlinx.parcelize.Parcelize

@Parcelize
data class AppInfo(
    val appName: String,
    val packageName: String,
    val isSystemApp: Boolean,
) : Parcelable

fun AppInfoEntity.asAppInfo(): AppInfo = AppInfo(
    appName = appName,
    packageName = packageName,
    isSystemApp = isSystemApp,
)
