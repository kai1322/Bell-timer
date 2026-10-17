package com.belltimer.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_LOCKED_BOOT_COMPLETED &&
            action != "android.intent.action.QUICKBOOT_POWERON") {
            return
        }

        Log.d(TAG, "Boot completed, checking timer state")
        val prefs = Prefs(context)
        if (!prefs.isRunning || prefs.startTimeMillis == 0L) {
            Log.d(TAG, "No active timer")
            return
        }

        // Reschedule next alarm from original start
        val intervalMs = prefs.intervalMinutes * 60_000L
        val next = AlarmScheduler.nextTriggerTime(prefs.startTimeMillis, intervalMs)
        prefs.nextAlarmTime = next
        AlarmScheduler.scheduleNext(context, next)
        NotificationHelper.showOrUpdate(context, next)
        TimerService.start(context)
        Log.d(TAG, "Rescheduled next alarm at $next")
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
