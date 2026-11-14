package io.wiger.timeup.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.wiger.timeup.data.local.entity.TimeUpEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TimeUpDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimeUps(vararg timeUp: TimeUpEntity)

    @Update
    suspend fun updateTimeUp(vararg timeUp: TimeUpEntity)

    @Query("DELETE FROM time_up_entries WHERE id = :id")
    suspend fun removeTimeUp(id: Long)

    @Query("SELECT * from time_up_entries ORDER BY id ASC")
    fun loadAllTimeUps(): Flow<List<TimeUpEntity>>

    @Query("SELECT * FROM time_up_entries WHERE alert_status = 1")
    suspend fun loadEnabledTimeUps(): List<TimeUpEntity>
}
