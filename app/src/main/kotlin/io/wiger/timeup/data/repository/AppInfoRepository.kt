package io.wiger.timeup.data.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import io.wiger.timeup.data.datasource.InstalledAppListSource
import io.wiger.timeup.data.local.dao.AppInfoDao
import io.wiger.timeup.data.local.entity.AppInfoEntity
import io.wiger.timeup.data.model.AppInfo
import io.wiger.timeup.data.model.asAppInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppInfoRepository @Inject constructor(
    @param:ApplicationContext val context: Context,
    private val appInfoDao: AppInfoDao,
    private val installedAppListSource: InstalledAppListSource,
) {

    fun getInstalledAppList(isSystemApp: Boolean): Flow<List<AppInfo>> =
        appInfoDao.getAppInfos(isSystemApp)
            .map { it.map(AppInfoEntity::asAppInfo) }

    suspend fun refreshAppList() {
        appInfoDao.cleanAll()
        val applicationInfos = installedAppListSource.appInfoList()
        applicationInfos.map { it.asAppInfoEntity(context) }.toTypedArray().let(appInfoDao::insert)
    }
}

private fun ApplicationInfo.asAppInfoEntity(context: Context): AppInfoEntity {
    val packageManager = context.packageManager
    val appName = packageManager.getApplicationLabel(this).toString()
    val isSystemApp = flags and ApplicationInfo.FLAG_SYSTEM != 0
    return AppInfoEntity(packageName, appName, isSystemApp)
}
