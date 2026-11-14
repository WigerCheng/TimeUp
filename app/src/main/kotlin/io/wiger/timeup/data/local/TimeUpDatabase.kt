package io.wiger.timeup.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import io.wiger.timeup.data.local.dao.AppInfoDao
import io.wiger.timeup.data.local.dao.TimeUpDao
import io.wiger.timeup.data.local.entity.AppInfoEntity
import io.wiger.timeup.data.local.entity.TimeUpEntity

@Database(
    entities = [TimeUpEntity::class, AppInfoEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class TimeUpDatabase : RoomDatabase() {
    abstract fun timeUpDao(): TimeUpDao
    abstract fun appInfoDao(): AppInfoDao
}
