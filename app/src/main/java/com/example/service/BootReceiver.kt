package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.remote.LeetCodeRemoteProvider
import com.example.data.repository.StreakSentinelRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "BootReceiver triggered by action: $action")

        val pendingResult = goAsync()
        val scope = CoroutineScope(Dispatchers.IO)

        scope.launch {
            try {
                val database = AppDatabase.getInstance(context)
                val remote = LeetCodeRemoteProvider()
                val repository = StreakSentinelRepository(database, remote)
                val scheduler = AlarmScheduler(context)
                val settings = repository.getSettings()

                if (settings.isMonitoringEnabled) {
                    scheduler.scheduleDailyCutoffAlarm(settings)
                    scheduler.schedulePeriodicWork(settings.checkIntervalMinutes)
                    repository.logEvent(
                        eventType = "RECOVERY",
                        resultStatus = "RESTORED",
                        details = "Restored alarm and monitoring schedules after $action"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error restoring schedules in BootReceiver", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
