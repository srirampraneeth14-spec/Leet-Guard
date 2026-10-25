package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.AlarmSessionDao
import com.example.data.local.dao.DailyActivityDao
import com.example.data.local.dao.MonitoringEventDao
import com.example.data.local.dao.UserSettingsDao
import com.example.data.local.entity.AlarmSessionEntity
import com.example.data.local.entity.DailyActivityEntity
import com.example.data.local.entity.MonitoringEventEntity
import com.example.data.local.entity.UserSettingsEntity

@Database(
    entities = [
        UserSettingsEntity::class,
        DailyActivityEntity::class,
        MonitoringEventEntity::class,
        AlarmSessionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userSettingsDao(): UserSettingsDao
    abstract fun dailyActivityDao(): DailyActivityDao
    abstract fun monitoringEventDao(): MonitoringEventDao
    abstract fun alarmSessionDao(): AlarmSessionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "streak_sentinel_database"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
