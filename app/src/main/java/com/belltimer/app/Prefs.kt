package com.belltimer.app

import android.content.Context
import android.content.SharedPreferences

class Prefs(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("bell_timer_prefs", Context.MODE_PRIVATE)

    var isRunning: Boolean
        get() = prefs.getBoolean(KEY_RUNNING, false)
        set(value) = prefs.edit().putBoolean(KEY_RUNNING, value).apply()

    var startTimeMillis: Long
        get() = prefs.getLong(KEY_START_TIME, 0L)
        set(value) = prefs.edit().putLong(KEY_START_TIME, value).apply()

    var intervalMinutes: Int
        get() = prefs.getInt(KEY_INTERVAL, 5)
        set(value) = prefs.edit().putInt(KEY_INTERVAL, value).apply()

    var beepCount: Int
        get() = prefs.getInt(KEY_BEEP_COUNT, 2)
        set(value) = prefs.edit().putInt(KEY_BEEP_COUNT, value).apply()

    var volumePercent: Int
        get() = prefs.getInt(KEY_VOLUME, 80)
        set(value) = prefs.edit().putInt(KEY_VOLUME, value).apply()

    var nextAlarmTime: Long
        get() = prefs.getLong(KEY_NEXT_ALARM, 0L)
        set(value) = prefs.edit().putLong(KEY_NEXT_ALARM, value).apply()

    fun clearRunning() {
        prefs.edit()
            .putBoolean(KEY_RUNNING, false)
            .putLong(KEY_START_TIME, 0L)
            .putLong(KEY_NEXT_ALARM, 0L)
            .apply()
    }

    companion object {
        private const val KEY_RUNNING = "running"
        private const val KEY_START_TIME = "start_time"
        private const val KEY_INTERVAL = "interval_minutes"
        private const val KEY_BEEP_COUNT = "beep_count"
        private const val KEY_VOLUME = "volume"
        private const val KEY_NEXT_ALARM = "next_alarm"
    }
}
