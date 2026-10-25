package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.AlarmSessionEntity
import com.example.data.local.entity.DailyActivityEntity
import com.example.data.local.entity.MonitoringEventEntity
import com.example.data.local.entity.UserSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserSettingsDao {
    @Query("SELECT * FROM user_settings WHERE id = 1 LIMIT 1")
    fun getSettingsFlow(): Flow<UserSettingsEntity?>

    @Query("SELECT * FROM user_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettings(): UserSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(settings: UserSettingsEntity)
}

@Dao
interface DailyActivityDao {
    @Query("SELECT * FROM daily_activity WHERE date = :date LIMIT 1")
    fun getActivityFlow(date: String): Flow<DailyActivityEntity?>

    @Query("SELECT * FROM daily_activity WHERE date = :date LIMIT 1")
    suspend fun getActivity(date: String): DailyActivityEntity?

    @Query("SELECT * FROM daily_activity ORDER BY date DESC")
    fun getAllActivitiesFlow(): Flow<List<DailyActivityEntity>>

    @Query("SELECT * FROM daily_activity ORDER BY date DESC")
    suspend fun getAllActivities(): List<DailyActivityEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(activity: DailyActivityEntity)

    @Query("DELETE FROM daily_activity WHERE date = :date")
    suspend fun deleteActivity(date: String)

    @Query("DELETE FROM daily_activity")
    suspend fun deleteAll()
}

@Dao
interface MonitoringEventDao {
    @Query("SELECT * FROM monitoring_events ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentEventsFlow(limit: Int = 100): Flow<List<MonitoringEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: MonitoringEventEntity)

    @Query("DELETE FROM monitoring_events")
    suspend fun deleteAll()
}

@Dao
interface AlarmSessionDao {
    @Query("SELECT * FROM alarm_sessions WHERE date = :date LIMIT 1")
    fun getAlarmSessionFlow(date: String): Flow<AlarmSessionEntity?>

    @Query("SELECT * FROM alarm_sessions WHERE date = :date LIMIT 1")
    suspend fun getAlarmSession(date: String): AlarmSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(session: AlarmSessionEntity)

    @Query("DELETE FROM alarm_sessions")
    suspend fun deleteAll()
}
