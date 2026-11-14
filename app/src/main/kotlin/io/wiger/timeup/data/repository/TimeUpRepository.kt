package io.wiger.timeup.data.repository

import io.wiger.timeup.data.local.dao.TimeUpDao
import io.wiger.timeup.data.local.entity.TimeUpEntity
import io.wiger.timeup.data.model.TimeUp
import io.wiger.timeup.data.model.asTimeUp
import io.wiger.timeup.data.model.asTimeUpEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimeUpRepository @Inject constructor(
    private val dao: TimeUpDao,
) {

    val allTimeUpsFlow: Flow<List<TimeUp>> =
        dao.loadAllTimeUps().map { it.map(TimeUpEntity::asTimeUp) }

    suspend fun addTimeUp(timeUp: TimeUp) {
        val entry = timeUp.asTimeUpEntry()
        dao.insertTimeUps(entry)
    }

    suspend fun getEnabledTimeUps(): List<TimeUp> =
        dao.loadEnabledTimeUps().map(TimeUpEntity::asTimeUp)

    suspend fun updateTimeUp(timeUp: TimeUp) {
        val entry = timeUp.asTimeUpEntry()
        dao.updateTimeUp(entry)
    }

    suspend fun removeTimeUp(id: Long) {
        dao.removeTimeUp(id)
    }
}
