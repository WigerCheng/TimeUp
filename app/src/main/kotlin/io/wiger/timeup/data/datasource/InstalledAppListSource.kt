package io.wiger.timeup.data.datasource

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InstalledAppListSource @Inject constructor(@ApplicationContext context: Context) {

    private val packageManager: PackageManager = context.packageManager

    suspend fun appInfoList(): List<ApplicationInfo> = withContext(Dispatchers.IO) {
        packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            .filterNotNull()
    }
}