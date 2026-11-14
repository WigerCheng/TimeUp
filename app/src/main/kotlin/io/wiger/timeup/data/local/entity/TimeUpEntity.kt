package io.wiger.timeup.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "time_up_entries")
class TimeUpEntity(
    @PrimaryKey(autoGenerate = true) val id: Long? = null,
    @ColumnInfo(name = "alert_name") val alertName: String,
    @ColumnInfo(name = "alert_count_down_time") val alertCountDownTime: Long,
    @ColumnInfo(name = "alert_status") val alertStatus: Boolean = true,
    @ColumnInfo(name = "alert_open_app_package_name") val launchAppPackageName: String? = null,
    @ColumnInfo(name = "alert_ends_at_millis") val alertEndsAtMillis: Long? = null,
)
