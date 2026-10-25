package com.example.service

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

class AlarmSoundPlayer(private val context: Context) {

    companion object {
        private const val TAG = "AlarmSoundPlayer"
    }

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var isPlaying = false

    init {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun startAlarm(toneName: String, enableVibration: Boolean) {
        if (isPlaying) return
        isPlaying = true

        try {
            val alarmUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, alarmUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start MediaPlayer for alarm", e)
        }

        if (enableVibration && vibrator != null) {
            try {
                val timings = longArrayOf(0, 600, 250, 600, 250, 900, 400)
                val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255, 0)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = VibrationEffect.createWaveform(timings, amplitudes, 0)
                    vibrator?.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(timings, 0)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start vibrator", e)
            }
        }
    }

    fun playSample(durationMs: Long = 3000L) {
        startAlarm("DEFAULT", true)
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            stopAlarm()
        }, durationMs)
    }

    fun stopAlarm() {
        isPlaying = false
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping MediaPlayer", e)
        } finally {
            mediaPlayer = null
        }

        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling vibrator", e)
        }
    }
}
