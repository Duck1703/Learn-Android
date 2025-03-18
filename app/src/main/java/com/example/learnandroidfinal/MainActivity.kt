package com.example.learnandroidfinal

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.learnandroidfinal.databinding.ActivityMainBinding
import com.example.learnandroidfinal.receivers.WaterReminderReceiver
import com.example.learnandroidfinal.service.HealthDataService
import com.example.learnandroidfinal.service.StepCounterService
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val WATER_REMINDER_INTERVAL = 1 // phút
    }

    private lateinit var binding: ActivityMainBinding
    private var healthDataService: HealthDataService? = null
    private var isServiceBound = false
    private var isReminderEnabled = false

    // ServiceConnection để kết nối với HealthDataService
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as HealthDataService.HealthDataBinder
            healthDataService = binder.getService()
            isServiceBound = true

            // Bắt đầu lấy dữ liệu từ service
            observeHealthData()

            Log.d(TAG, "Service connected")
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            healthDataService = null
            isServiceBound = false
            Log.d(TAG, "Service disconnected")
        }
    }

    // Khởi tạo Activity Launcher để yêu cầu quyền
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }

        if (allGranted) {
            startHealthTracking()
        } else {
            Toast.makeText(
                this,
                "Cần có quyền này để ứng dụng hoạt động chính xác",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupListeners()
        // Kết nối tới HealthDataService
        bindHealthDataService()
    }

    private fun setupListeners() {
        binding.startTrackingButton.setOnClickListener {
            checkPermissionsAndStartTracking()
        }

        binding.stopTrackingButton.setOnClickListener {
            stopHealthTracking()
        }

        binding.enableReminderButton.setOnClickListener {
            enableWaterReminder()
        }

        binding.disableReminderButton.setOnClickListener {
            disableWaterReminder()
        }
    }

    private fun checkPermissionsAndStartTracking() {
        val permissions = mutableListOf<String>()

        // Kiểm tra quyền ACTIVITY_RECOGNITION
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACTIVITY_RECOGNITION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.ACTIVITY_RECOGNITION)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Yêu cầu quyền nếu cần, hoặc bắt đầu dịch vụ ngay
        if (permissions.isNotEmpty()) {
            requestPermissionLauncher.launch(permissions.toTypedArray())
        } else {
            startHealthTracking()
        }
    }

    private fun startHealthTracking() {
        // Khởi động StepCounterService (Foreground Service)
        val serviceIntent = Intent(this, StepCounterService::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        Toast.makeText(this, "Bắt đầu theo dõi sức khỏe", Toast.LENGTH_SHORT).show()
    }

    private fun stopHealthTracking() {
        // Dừng StepCounterService
        val serviceIntent = Intent(this, StepCounterService::class.java)
        stopService(serviceIntent)

        Toast.makeText(this, "Dừng theo dõi sức khỏe", Toast.LENGTH_SHORT).show()
    }

    private fun bindHealthDataService() {
        val intent = Intent(this, HealthDataService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        startService(intent)
    }

    private fun observeHealthData() {
        lifecycleScope.launch {
            healthDataService?.stepsCount?.collect { steps ->
                binding.stepsCountTextView.text = steps.toString()
            }
        }

        lifecycleScope.launch {
            healthDataService?.activeTimeMinutes?.collect { time ->
                val hours = time / 60
                val minutes = time % 60
                binding.activeTimeTextView.text = "${hours}h ${minutes}m"
            }
        }
    }

    @SuppressLint("ObsoleteSdkInt", "ScheduleExactAlarm")
    private fun enableWaterReminder() {
        if (isReminderEnabled) return

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, WaterReminderReceiver::class.java).apply {
            action = WaterReminderReceiver.ACTION_WATER_REMINDER
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Thiết lập lịch nhắc nhở định kỳ
        val intervalMillis = TimeUnit.MINUTES.toMillis(WATER_REMINDER_INTERVAL.toLong())
        val triggerTime = System.currentTimeMillis() + intervalMillis

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }

        isReminderEnabled = true
        binding.waterReminderTextView.text =
            "Water Reminder: Enabled ($WATER_REMINDER_INTERVAL phút)"
        Toast.makeText(this, "Đã bật nhắc nhở uống nước", Toast.LENGTH_SHORT).show()
    }

    private fun disableWaterReminder() {
        if (!isReminderEnabled) return

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, WaterReminderReceiver::class.java).apply {
            action = WaterReminderReceiver.ACTION_WATER_REMINDER
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Hủy lịch nhắc nhở
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()

        isReminderEnabled = false
        binding.waterReminderTextView.text = "Water Reminder: Disabled"
        Toast.makeText(this, "Đã tắt nhắc nhở uống nước", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isServiceBound) {
            unbindService(serviceConnection)
            isServiceBound = false
        }
    }
}
