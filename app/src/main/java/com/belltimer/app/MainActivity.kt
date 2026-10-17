package com.belltimer.app

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.belltimer.app.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: Prefs

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        if (!allGranted) {
            Toast.makeText(this, getString(R.string.permission_needed), Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = Prefs(this)
        NotificationHelper.createChannel(this)

        setupUi()
        requestNeededPermissions()
        updateUiFromPrefs()
    }

    override fun onResume() {
        super.onResume()
        updateUiFromPrefs()
    }

    private fun setupUi() {
        // Beep count toggle
        when (prefs.beepCount) {
            1 -> binding.toggleBeepCount.check(R.id.btnBeep1)
            3 -> binding.toggleBeepCount.check(R.id.btnBeep3)
            else -> binding.toggleBeepCount.check(R.id.btnBeep2)
        }

        binding.toggleBeepCount.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                prefs.beepCount = when (checkedId) {
                    R.id.btnBeep1 -> 1
                    R.id.btnBeep3 -> 3
                    else -> 2
                }
            }
        }

        // Volume
        binding.seekVolume.progress = prefs.volumePercent
        binding.tvVolume.text = "${prefs.volumePercent}%"
        binding.seekVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.tvVolume.text = "$progress%"
                if (fromUser) prefs.volumePercent = progress
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Interval
        binding.etInterval.setText(prefs.intervalMinutes.toString())

        // Start / Stop
        binding.btnStartStop.setOnClickListener {
            if (prefs.isRunning) {
                stopTimer()
            } else {
                startTimer()
            }
        }

        // Battery
        binding.btnBattery.setOnClickListener { requestBatteryOptimization() }
        binding.btnAutostart.setOnClickListener { openMiuiAutostart() }
    }

    private fun startTimer() {
        val intervalText = binding.etInterval.text?.toString()?.trim() ?: "5"
        val minutes = intervalText.toIntOrNull()?.coerceIn(1, 240) ?: 5
        prefs.intervalMinutes = minutes
        binding.etInterval.setText(minutes.toString())

        // Ensure exact alarm permission on Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!am.canScheduleExactAlarms()) {
                try {
                    startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = Uri.parse("package:$packageName")
                    })
                    Toast.makeText(this, "กรุณาอนุญาต Exact Alarm แล้วกดเริ่มอีกครั้ง", Toast.LENGTH_LONG).show()
                    return
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        val now = System.currentTimeMillis()
        prefs.startTimeMillis = now
        prefs.isRunning = true

        val intervalMs = minutes * 60_000L
        val next = now + intervalMs
        prefs.nextAlarmTime = next

        AlarmScheduler.scheduleNext(this, next)
        NotificationHelper.showOrUpdate(this, next)
        TimerService.start(this)

        updateUiFromPrefs()
        Toast.makeText(this, "เริ่มแล้ว — ครั้งแรกใน $minutes นาที", Toast.LENGTH_SHORT).show()
    }

    private fun stopTimer() {
        prefs.clearRunning()
        AlarmScheduler.cancel(this)
        NotificationHelper.cancel(this)
        TimerService.stop(this)
        updateUiFromPrefs()
        Toast.makeText(this, "หยุดแล้ว", Toast.LENGTH_SHORT).show()
    }

    @SuppressLint("SetTextI18n")
    private fun updateUiFromPrefs() {
        if (prefs.isRunning && prefs.nextAlarmTime > 0) {
            val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                .format(Date(prefs.nextAlarmTime))
            binding.tvStatus.text = getString(R.string.status_running, timeStr)
            binding.btnStartStop.text = getString(R.string.stop)
            binding.btnStartStop.backgroundTintList =
                ContextCompat.getColorStateList(this, R.color.stop_red)
            binding.etInterval.isEnabled = false
            binding.toggleBeepCount.isEnabled = false
        } else {
            binding.tvStatus.text = getString(R.string.status_idle)
            binding.btnStartStop.text = getString(R.string.start)
            binding.btnStartStop.backgroundTintList =
                ContextCompat.getColorStateList(this, R.color.start_green)
            binding.etInterval.isEnabled = true
            binding.toggleBeepCount.isEnabled = true
        }
    }

    private fun requestNeededPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (permissions.isNotEmpty()) {
            requestPermissionLauncher.launch(permissions.toTypedArray())
        }
    }

    private fun requestBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    // Fallback to general settings
                    try {
                        startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                    } catch (e2: Exception) {
                        Toast.makeText(this, "เปิดการตั้งค่าแบตเตอรี่ด้วยตัวเอง", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "ยกเว้นแล้ว", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Try to open MIUI / Xiaomi / Redmi Autostart management page.
     * These intents are not official and may change between MIUI versions.
     */
    private fun openMiuiAutostart() {
        val intents = listOf(
            Intent().setClassName(
                "com.miui.securitycenter",
                "com.miui.permcenter.autostart.AutoStartManagementActivity"
            ),
            Intent("miui.intent.action.OP_AUTO_START").addCategory(Intent.CATEGORY_DEFAULT),
            Intent().setClassName(
                "com.miui.securitycenter",
                "com.miui.powercenter.PowerSettings"
            ),
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            }
        )
        for (intent in intents) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                return
            } catch (e: Exception) {
                // try next
            }
        }
        Toast.makeText(this, "เปิดการตั้งค่าแอปด้วยตัวเอง แล้วอนุญาตเริ่มอัตโนมัติ", Toast.LENGTH_LONG).show()
    }
}
