# LEET GUARD — TECHNICAL AUDIT REPORT

**Date of Audit:** October 25, 2026  
**Application Name:** Leet Guard  
**Target Package:** `com.aistudio.streaksentinel.krvpzt` (Namespace: `com.example`)  
**Repository State:** Production-Ready Native Android Codebase  

---

## 1. Executive Summary

This audit document details the concrete architecture, runtime dependencies, APIs, persistence structures, background schedulers, and verification mechanisms present in the **Leet Guard** codebase.

The project is built as a **100% native Android application** using **Kotlin 2.2.10** and **Jetpack Compose (Material 3)**. It contains no web wrappers, hybrid bridges, or mock simulated data layers.

---

## 2. Technology Stack & Framework Inventory

### 2.1 Application Type & Platform
* **Classification:** Native Android Application (APK/AAB).
* **Target Platforms:** Android 7.0 (API level 24, Nougat) through Android 15/16 (API level 36).
* **Evidence:** `/app/build.gradle.kts`
  ```kotlin
  android {
      namespace = "com.example"
      compileSdk { version = release(36) { minorApiLevel = 1 } }
      defaultConfig {
          applicationId = "com.aistudio.streaksentinel.krvpzt"
          minSdk = 24
          targetSdk = 36
          versionCode = 1
          versionName = "1.0"
      }
  }
  ```
* **Project Name:** `Leet Guard` (`settings.gradle.kts` Line 25: `rootProject.name = "Leet Guard"`).
* **Launcher String:** `Leet Guard` (`app/src/main/res/values/strings.xml` Line 2: `<string name="app_name">Leet Guard</string>`).

### 2.2 Programming Languages
* **Kotlin (`.kt`):** 100% of application code, services, background workers, and unit tests.
* **Kotlin DSL (`.gradle.kts`):** Build scripting in root and module build files.
* **TOML (`libs.versions.toml`):** Modern Gradle Version Catalog.
* **XML:** Android resource system (`AndroidManifest.xml`, vector drawables, adaptive launcher metadata).

### 2.3 User Interface Framework
* **Toolkit:** Jetpack Compose with Material Design 3 (M3).
* **Compose BOM:** `2024.09.00`.
* **Layouts:** `Scaffold`, `NavigationBar`, `TopAppBar`, `LazyColumn`, `Box`, `Card`, and Compose Canvas elements.
* **Edge-to-Edge:** Native insets handling with `enableEdgeToEdge()` in `MainActivity.kt` and `windowInsetsPadding(WindowInsets.navigationBars)`.
* **Theme Tokens:** Dynamic M3 palette in `Theme.kt` and `Color.kt` featuring Dark Navy (`#0B0F19`), Verified Emerald (`#10B981`), Warning Amber (`#F59E0B`), and Cutoff Crimson (`#EF4444`).

### 2.4 Build System & Code Generation
* **Build System:** Gradle (AGP 9.1.1).
* **Code Generator:** KSP (Kotlin Symbol Processing) `2.3.5` for Room database compiler (`androidx-room-compiler`).
* **Java Target:** JVM 11 (`sourceCompatibility = JavaVersion.VERSION_11`).

### 2.5 Backend Architecture
* **Serverless Client Architecture:** No proprietary intermediate backend, proxy server, or cloud account is required. The Android client directly communicates with public endpoints.

---

## 3. Database & Local Storage Layer

* **Storage Engine:** Room Database 2.7.0 backed by SQLite (`/app/src/main/java/com/example/data/local/AppDatabase.kt`).
* **Database Name:** `streak_sentinel_database`.

### Schema & Entities (`Entities.kt`)

1. **`user_settings` Entity:**
   * `id`: Int (Singleton primary key = 1).
   * `leetcodeUsername`: String.
   * `isMonitoringEnabled`: Boolean (Default: true).
   * `startHour`: Int (Default: 9) & `startMinute`: Int (Default: 0).
   * `cutoffHour`: Int (Default: 22) & `cutoffMinute`: Int (Default: 30).
   * `checkIntervalMinutes`: Int (Default: 30).
   * `timezoneId`: String (Default: `"Asia/Kolkata"`).
   * `snoozeMinutes`: Int (Default: 5).
   * `maxSnoozes`: Int (Default: 3).
   * `alarmTone`: String (Default: `"EMERGENCY_SIREN"`).
   * `isVibrationEnabled`: Boolean (Default: true).
   * `isEscalationEnabled`: Boolean (Default: true).
   * `failSafePolicy`: String (`"PROTECTIVE"` vs `"CONSERVATIVE"`).
   * `unverifiedPastStreak`: Int (Default: 285).
   * `isOnboardingCompleted`: Boolean.
   * `lastSyncTimestamp`: Long.

2. **`daily_activity` Entity:**
   * `date`: String (Primary Key, ISO `YYYY-MM-DD`).
   * `verificationStatus`: String (`VERIFIED_ACCEPTED`, `ACTIVITY_FOUND_ACCEPTANCE_UNKNOWN`, `NO_QUALIFYING_SUBMISSION_FOUND`, `VERIFICATION_UNAVAILABLE`, `PROFILE_INVALID`).
   * `evidenceSource`: String (`AUTOMATIC_LEETCODE_API`, `MANUAL_CONFIRMATION`, `SIMULATED_TEST_MODE`).
   * `acceptedSubmissionTimestamp`: Long? (Remote submission epoch millis).
   * `problemTitle`: String?
   * `submissionId`: String?
   * `lastCheckedAt`: Long.
   * `manualConfirmationTimestamp`: Long?
   * `notes`: String?

3. **`monitoring_events` Entity (Audit Trail):**
   * `id`: Long (Autoincrement PK).
   * `timestamp`: Long.
   * `eventType`: String (`CHECK_VERIFIED`, `CUTOFF_REACHED`, `ALARM_SNOOZED`, `ALARM_STOPPED`, `MANUAL_CONFIRMATION`, `RECOVERY`).
   * `resultStatus`: String.
   * `details`: String.
   * `isSimulated`: Boolean.

4. **`alarm_sessions` Entity:**
   * `date`: String (Primary Key).
   * `status`: String (`IDLE`, `ACTIVE`, `SNOOZED`, `DISMISSED`).
   * `escalationStage`: Int (1 = Routine, 2 = Approaching, 3 = Cutoff, 4 = Alarm Active).
   * `snoozeCount`: Int.
   * `startedAt`: Long?, `lastTriggeredAt`: Long?, `stoppedAt`: Long?.

---

## 4. LeetCode Integration Layer

* **Interface:** `LeetCodeActivityProvider` (`/app/src/main/java/com/example/data/remote/LeetCodeModels.kt`).
* **Implementation:** `LeetCodeRemoteProvider` (`/app/src/main/java/com/example/data/remote/LeetCodeRemoteProvider.kt`).
* **Protocol & Endpoint:** HTTPS POST to `https://leetcode.com/graphql`.
* **User-Agent:** `Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36 LeetGuard/1.0`.
* **Client Configuration:** OkHttp 4.10.0 with 15-second connect/read/write timeouts.

### Queries Implemented
1. **Profile Validation Query:**
   ```graphql
   query getUserProfile($username: String!) {
       matchedUser(username: $username) {
           username
           profile { realName userAvatar ranking }
           submitStats { acSubmissionNum { difficulty count } }
       }
   }
   ```
2. **Recent Submissions Query:**
   ```graphql
   query getRecentSubmissions($username: String!, $limit: Int!) {
       recentAcSubmissionList(username: $username, limit: $limit) {
           id title titleSlug timestamp
       }
   }
   ```

### Verification Logic & Timezone Handling
* Submissions provide a Unix timestamp in seconds (`timestamp`).
* Converted to the user's configured local calendar date:
  ```kotlin
  val subDate = Instant.ofEpochSecond(sub.timestampSeconds)
      .atZone(zoneId)
      .toLocalDate()
  subDate == targetDate
  ```
* **Decision Types:**
  * `VERIFIED_ACCEPTED`: Evidence found; contains problem title, ID, and timestamp.
  * `NO_QUALIFYING_SUBMISSION_FOUND`: Profile queried successfully, no accepted submission matches target date.
  * `VERIFICATION_UNAVAILABLE`: Network failure, timeout, or rate-limiting (HTTP 429). Preserves uncertainty rather than claiming no submission exists.
  * `PROFILE_INVALID`: Username not found or blank.

### Manual Confirmation Fallback
* Users can manually mark today's activity complete if LeetCode is unreachable.
* Stored with `evidenceSource = MANUAL_CONFIRMATION`.
* If automatic verification later succeeds, it non-destructively upgrades the record to `VERIFIED_ACCEPTED` with problem evidence without creating duplicate daily rows.

---

## 5. Background Scheduling & Escalation

### 5.1 Periodic Background Polling
* **Technology:** AndroidX WorkManager 2.10.0 (`/app/src/main/java/com/example/service/SentinelWorker.kt`).
* **Policy:** Unique periodic work (`SentinelPeriodicWork`, `ExistingPeriodicWorkPolicy.UPDATE`).
* **Constraints:** Network connectivity required (`NetworkType.CONNECTED`).
* **Execution Window:** Checks whether the current time is within `startHour:startMinute` and `cutoffHour:cutoffMinute`.

### 5.2 Exact Cutoff Alarms
* **Technology:** Android AlarmManager (`/app/src/main/java/com/example/service/AlarmScheduler.kt`).
* **Wakeup Method:** `AlarmManager.setExactAndAllowWhileIdle()` when `canScheduleExactAlarms()` is granted, falling back to `setAndAllowWhileIdle()`.
* **Pre-Alarm Recheck:** When the cutoff alarm fires, `AlarmReceiver` triggers an immediate network verification re-check. If today's problem was solved in the interim, the alarm automatically disarms and sends a completion notification.

### 5.3 System Boot & Time Change Recovery
* **Technology:** BroadcastReceiver (`/app/src/main/java/com/example/service/BootReceiver.kt`).
* **Actions Handled:**
  * `android.intent.action.BOOT_COMPLETED`
  * `android.intent.action.MY_PACKAGE_REPLACED`
  * `android.intent.action.TIME_SET`
  * `android.intent.action.TIMEZONE_CHANGED`
* **Recovery Behavior:** Reschedules the exact cutoff alarm and reenqueues WorkManager periodic checks based on persistent settings in Room.

---

## 6. Notifications, Audio & Vibration

### 6.1 Notification Channels (`NotificationHelper.kt`)
1. `channel_routine_reminders`: Default importance, vibration enabled, titles: `Leet Guard: Daily Reminder`, actions: *Check Now*, *Open LeetCode*, *Mark Done*.
2. `channel_cutoff_warnings`: High importance, titles: `Leet Guard: Cutoff in X Minutes!`, warning vibration pattern `[0, 400, 200, 400]`.
3. `channel_urgent_alarms`: High importance, alarm audio attributes, urgent vibration pattern `[0, 600, 300, 600, 300, 1000]`.
4. `channel_completion_status`: Low importance, silent confirmation when streak is verified.

### 6.2 Alarm Audio & Foreground Service
* **Service:** `AlarmService` (`/app/src/main/java/com/example/service/AlarmService.kt`).
  * Runs as a foreground service with `ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK`.
  * Ongoing notification with `setFullScreenIntent`.
* **Audio Engine:** `AlarmSoundPlayer` (`/app/src/main/java/com/example/service/AlarmSoundPlayer.kt`).
  * `MediaPlayer` configured with `AudioAttributes.USAGE_ALARM` in continuous looping mode.
  * System `Vibrator` / `VibratorManager` executing looping waveform vibrations.
* **Notification Actions:** `NotificationActionReceiver` handles *Snooze 5m*, *Stop Alarm*, *Check Now*, *Open LeetCode*, and *Mark Complete* directly from status bar notifications.

---

## 7. Streak Calculation & Analytics

* **Engine:** `StreakSentinelRepository.calculateStreakStats()` (`/app/src/main/java/com/example/data/repository/StreakSentinelRepository.kt`).
* **Streak Rules:**
  * Consecutive days are calculated backwards from today (if solved) or yesterday (if today is still pending).
  * Missing a day terminates the current streak.
  * Multiple submissions on the same day count as 1 streak day.
  * Previous personal records (e.g., 285-day streak) are preserved in `unverifiedHistoricalBest` as context without falsifying verified records.
* **Heatmap Matrix:** 10-week contribution grid (70 days) rendered in `StatisticsScreen.kt` and 14-day quick view in `DashboardScreen.kt`.
* **Export:** `StreakSentinelRepository.exportHistoryAsCsv()` formats all daily records for copy-to-clipboard or Android share intent with title `Leet Guard CSV`.

---

## 8. Automated Testing & Verification Status

### Unit & Robolectric Tests Executed
* All tests compiled and passed (`BUILD SUCCESSFUL` via `gradle :app:testDebugUnitTest`).
1. `ExampleRobolectricTest`: Verifies context string loading (`R.string.app_name` = `"Leet Guard"`).
2. `StreakSentinelUnitTest`:
   * Verified accepted submission date matching in target timezones (Asia/Kolkata).
   * Midnight boundary test confirming yesterday's submission does not verify today.
   * Backward streak calculation across verified and manual entries.
   * Non-destructive upgrade from manual confirmation to verified accepted submission.
   * Fail-safe policy resolution (`PROTECTIVE` vs `CONSERVATIVE`).

---

## 9. Recommended Next Steps for Field Deployment

1. **Local Raw Audio Asset:** Place a bundled `.mp3` or `.ogg` file in `app/src/main/res/raw/alarm_siren.mp3` to ensure loud playback even if a device has silent default alarm tones.
2. **Direct Battery Optimization Intent:** Provide a shortcut button in the Diagnostics screen triggering `Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`.
3. **Secondary REST Mirror:** Add a secondary fallback endpoint in `LeetCodeRemoteProvider` if LeetCode GraphQL ever experiences regional rate limits.
