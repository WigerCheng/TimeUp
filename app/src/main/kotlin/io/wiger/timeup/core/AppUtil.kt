package io.wiger.timeup.core

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.net.toUri
import androidx.core.os.LocaleListCompat
import androidx.core.os.bundleOf

object AppUtil {
    const val DONT_KILL_MY_APP_URL = "https://dontkillmyapp.com/"

    private const val EXTRA_OPPO_PERMISSION_LIST = "permissionList"
    private const val EXTRA_OPPO_IS_GET_PERMISSION = "isGetPermission"
    private const val ACTION_VIEW_ADVANCED_POWER_USAGE_DETAIL =
        "android.settings.VIEW_ADVANCED_POWER_USAGE_DETAIL"

    @JvmStatic
    fun getAppLanguageTag(): String? =
        AppCompatDelegate.getApplicationLocales()
            .toLanguageTags()
            .takeIf { it.isNotEmpty() }

    @JvmStatic
    fun setAppLanguage(languageTag: String) {
        val locales = if (languageTag.isBlank()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(languageTag)
        }
        AppCompatDelegate.setApplicationLocales(locales)
    }

    @JvmStatic
    fun toPermissionSettingPage(context: Context, vararg permissions: String) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.fromParts("package", context.packageName, null)
        val bundle = bundleOf()
        val permissionsAsArrayList: ArrayList<String> = ArrayList(permissions.toList())
        bundle.putStringArrayList(EXTRA_OPPO_PERMISSION_LIST, permissionsAsArrayList)
        intent.putExtras(bundle)
        intent.putExtra(EXTRA_OPPO_IS_GET_PERMISSION, true)
        context.startActivity(intent)
    }

    @SuppressLint("BatteryLife")
    @JvmStatic
    fun toAppDetailsPage(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        context.startActivity(intent)
    }


    @JvmStatic
    fun toBatteryUsageDetailPage(context: Context) {
        val intent = Intent(ACTION_VIEW_ADVANCED_POWER_USAGE_DETAIL).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        runCatching { context.startActivity(intent) }
            .onFailure { toAppDetailsPage(context) }
    }

    @JvmStatic
    fun openUrl(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
        runCatching { context.startActivity(intent) }
    }

    @SuppressLint("BatteryLife")
    @JvmStatic
    fun toIgnoreBatteryOptimizationsSetting(context: Context) {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
        intent.data = "package:${context.packageName}".toUri()
        context.startActivity(intent)
    }

    @JvmStatic
    fun toNotificationSettingPage(context: Context) {
        val notificationIntent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
        context.startActivity(notificationIntent)
    }


    @JvmStatic
    fun toLiveUpdateSettingPage(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.BAKLAVA) return
        val promotionIntent = Intent(Settings.ACTION_APP_NOTIFICATION_PROMOTION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
        context.startActivity(promotionIntent)
    }
}