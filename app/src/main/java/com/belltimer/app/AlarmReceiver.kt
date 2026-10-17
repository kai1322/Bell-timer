package com.belltimer.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_ALARM) return

        Log.d(TAG, "Alarm fired")
        val prefs = Prefs(context)
        if (!prefs.isRunning) {
            Log.d(TAG, "Not running, ignore")
            return
        }

        // Play sound
        val player = SoundPlayer(context)
        player.play(prefs.beepCount, prefs.volumePercent)

        // Schedule next based on original start time (no drift)
        val intervalMs = prefs.intervalMinutes * 60_000L
        val next = AlarmScheduler.nextTriggerTime(prefs.startTimeMillis, intervalMs)
        prefs.nextAlarmTime = next
        AlarmScheduler.scheduleNext(context, next)

        // Update notification
        NotificationHelper.showOrUpdate(context, next)

        // Ensure service is running (for OEM reliability)
        TimerService.start(context)
    }

    companion object {
        const val ACTION_ALARM = "com.belltimer.app.ACTION_ALARM"
        private const val TAG = "AlarmReceiver"
    }
}
