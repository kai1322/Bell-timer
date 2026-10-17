package com.belltimer.app

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Lightweight foreground service to improve reliability on aggressive OEMs (Xiaomi etc.)
 * while the repeating timer is active.
 */
class TimerService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val prefs = Prefs(this)
        if (!prefs.isRunning) {
            stopSelf()
            return START_NOT_STICKY
        }

        val next = prefs.nextAlarmTime
        val notification = NotificationHelper.buildForService(this, next)
        startForeground(NotificationHelper.NOTIFICATION_ID, notification)
        Log.d(TAG, "Foreground service started")
        return START_STICKY
    }

    override fun onDestroy() {
        Log.d(TAG, "Service destroyed")
        super.onDestroy()
    }

    companion object {
        private const val TAG = "TimerService"

        fun start(context: Context) {
            val intent = Intent(context, TimerService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, TimerService::class.java))
        }
    }
}
