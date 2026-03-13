package com.attentionbeeper

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BeepService : Service() {

    companion object {
        private const val TAG = "BeepService"
        private const val CHANNEL_ID = "attention_beeper_channel"
        private const val NOTIFICATION_ID = 1
        const val ACTION_STOP = "com.attentionbeeper.ACTION_STOP"
    }

    inner class BeepBinder : Binder() {
        fun getService(): BeepService = this@BeepService
    }

    private val binder = BeepBinder()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var sessionJob: Job? = null

    // Mirrors session state for UI observation
    private val _sessionRunning = MutableStateFlow(false)
    val sessionRunning: StateFlow<Boolean> = _sessionRunning.asStateFlow()

    private val _intervalValue = MutableStateFlow(60)
    val intervalValue: StateFlow<Int> = _intervalValue.asStateFlow()

    private val _intervalUnit = MutableStateFlow(IntervalUnit.SECONDS)
    val intervalUnit: StateFlow<IntervalUnit> = _intervalUnit.asStateFlow()

    private val _intervalMode = MutableStateFlow(IntervalMode.RANDOM)
    val intervalMode: StateFlow<IntervalMode> = _intervalMode.asStateFlow()

    private val _selectedSound = MutableStateFlow("digital")
    val selectedSound: StateFlow<String> = _selectedSound.asStateFlow()

    private val _countdown = MutableStateFlow(-1L)
    val countdown: StateFlow<Long> = _countdown.asStateFlow()

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSession()
            return START_STICKY
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    fun startSession(
        intervalValue: Int,
        intervalUnit: IntervalUnit,
        intervalMode: IntervalMode,
        selectedSound: String
    ) {
        if (_sessionRunning.value) return

        _intervalValue.value = intervalValue
        _intervalUnit.value = intervalUnit
        _intervalMode.value = intervalMode
        _selectedSound.value = selectedSound
        _sessionRunning.value = true

        showNotification()

        val intervalMs = if (intervalUnit == IntervalUnit.SECONDS) {
            intervalValue.toLong() * 1000L
        } else {
            intervalValue.toLong() * 60_000L
        }

        sessionJob = scope.launch {
            runSession(intervalMs, intervalMode, selectedSound)
        }

        Log.d(TAG, "Session started: interval=${intervalMs}ms, mode=$intervalMode, sound=$selectedSound")
    }

    fun stopSession() {
        sessionJob?.cancel()
        sessionJob = null
        _sessionRunning.value = false
        _countdown.value = -1L
        cancelNotification()
        Log.d(TAG, "Session stopped")
    }

    private suspend fun runSession(intervalMs: Long, mode: IntervalMode, soundId: String) {
        val random = java.util.Random()
        while (true) {
            val windowStart = System.currentTimeMillis()
            val beepOffset = if (mode == IntervalMode.RANDOM) {
                (random.nextDouble() * intervalMs).toLong()
            } else {
                intervalMs
            }

            // Count down to beep within this window
            val beepTime = windowStart + beepOffset
            var remaining = beepTime - System.currentTimeMillis()
            while (remaining > 0) {
                _countdown.value = remaining
                val sleepMs = remaining.coerceAtMost(100L)
                delay(sleepMs)
                remaining = beepTime - System.currentTimeMillis()
            }
            _countdown.value = 0L

            // Play the beep
            SoundSynthesizer.play(soundId)

            if (mode == IntervalMode.FIXED) {
                // In fixed mode, next window starts from beep time (exact interval)
                // Already waited intervalMs from windowStart, so continue immediately
            } else {
                // In random mode: wait out the rest of the window
                val windowEnd = windowStart + intervalMs
                val waitRemaining = windowEnd - System.currentTimeMillis()
                if (waitRemaining > 0) {
                    _countdown.value = 0L
                    delay(waitRemaining)
                }
            }
        }
    }

    private fun showNotification() {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Attention Beeper",
            NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)

        val stopIntent = Intent(this, BeepService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_content))
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode_off)
            .setContentIntent(openPendingIntent)
            .addAction(
                Notification.Action.Builder(
                    null,
                    getString(R.string.notification_stop),
                    stopPendingIntent
                ).build()
            )
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun cancelNotification() {
        stopForeground(STOP_FOREGROUND_REMOVE)
    }
}
