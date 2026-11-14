package io.wiger.timeup.core

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * 参考文档：https://www.taf.org.cn/upload/AssociationStandard/TTAF%20108-2022%20%E7%A7%BB%E5%8A%A8%E7%BB%88%E7%AB%AF%E5%BA%94%E7%94%A8%E8%BD%AF%E4%BB%B6%E5%88%97%E8%A1%A8%E6%9D%83%E9%99%90%E5%AE%9E%E6%96%BD%E6%8C%87%E5%8D%97.pdf
 */
object ApplicationListUtil {
    const val GET_INSTALLED_APPS = "com.android.permission.GET_INSTALLED_APPS"

    private fun isSupportGetInstalledAppRuntimePermission(context: Context): Boolean = runCatching {
        context.packageManager.getPermissionInfo(
            GET_INSTALLED_APPS,
            0
        ) != null
    }.getOrElse { false }

    fun hasGetInstallAppPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return true
        if (!isSupportGetInstalledAppRuntimePermission(context)) return true
        return ContextCompat.checkSelfPermission(
            context,
            GET_INSTALLED_APPS
        ) == PackageManager.PERMISSION_GRANTED
    }
}