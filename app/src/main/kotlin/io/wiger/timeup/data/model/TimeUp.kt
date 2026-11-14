package io.wiger.timeup.data.model

import android.os.Parcelable
import androidx.compose.runtime.Stable
import io.wiger.timeup.data.local.entity.TimeUpEntity
import kotlinx.parcelize.Parcelize

@Stable
@Parcelize
data class TimeUp(
    val alertName: String = "",
    val alertTime: Long = 0,
    val isEnabled: Boolean = false,
    val appName: String? = null,
    val id: Long? = null,
    val endsAtMillis: Long? = null,
) : Parcelable

fun TimeUp.asTimeUpEntry(): TimeUpEntity {
    return TimeUpEntity(
        alertName = this.alertName,
        alertCountDownTime = this.alertTime,
        alertStatus = this.isEnabled,
        launchAppPackageName = appName,
        id = id,
        alertEndsAtMillis = endsAtMillis,
    )
}

fun TimeUpEntity.asTimeUp(): TimeUp = TimeUp(
    alertName = this.alertName,
    alertTime = this.alertCountDownTime,
    isEnabled = this.alertStatus,
    appName = this.launchAppPackageName,
    id = this.id,
    endsAtMillis = this.alertEndsAtMillis,
)
