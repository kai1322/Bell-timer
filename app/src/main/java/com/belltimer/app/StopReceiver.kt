package com.belltimer.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class StopReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Log.d("StopReceiver", "Stop requested from notification")
        val prefs = Prefs(context)
        prefs.clearRunning()
        AlarmScheduler.cancel(context)
        NotificationHelper.cancel(context)
        TimerService.stop(context)
    }
}
