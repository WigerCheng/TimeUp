package io.wiger.timeup.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.wiger.timeup.data.local.entity.AppInfoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppInfoDao {
    @Query(
        "SELECT * FROM app_infos " +
                "WHERE (:isSystemApps = 1 OR is_system_app = 0) " +
                "ORDER BY app_name ASC"
    )
    fun getAppInfos(
        isSystemApps: Boolean,
    ): Flow<List<AppInfoEntity>>

    @Query("DELETE FROM app_infos")
    fun cleanAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg appInfos: AppInfoEntity)
}