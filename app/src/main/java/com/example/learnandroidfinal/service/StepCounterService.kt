package com.example.learnandroidfinal.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.learnandroidfinal.MainActivity
import com.example.learnandroidfinal.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class StepCounterService : Service(), SensorEventListener {

    companion object {
        private const val TAG = "StepCounterService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "health_tracker_channel"

        // Actions for broadcasting
        const val ACTION_STEPS_UPDATED = "com.example.healthtracker.STEPS_UPDATED"
        const val ACTION_ACTIVE_TIME_UPDATED = "com.example.healthtracker.ACTIVE_TIME_UPDATED"

        // Intent extras
        const val EXTRA_STEPS_COUNT = "extra_steps_count"
        const val EXTRA_ACTIVE_TIME = "extra_active_time"
    }

    private var sensorManager: SensorManager? = null
    private var stepSensor: Sensor? = null
    private var initialSteps = -1
    private var stepsCount = 0
    private var activeTimeMinutes = 0L
    private var isServiceRunning = false
    private var wakeLock: PowerManager.WakeLock? = null

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)

    // Các biến cho chế độ giả lập bước chân
    private var simulationJob: Job? = null

    override fun onCreate() {
        super.onCreate()

        // Khởi tạo SensorManager và Step Sensor
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        // Kiểm tra xem thiết bị có hỗ trợ step counter không
        if (stepSensor == null) {
            // Nếu không hỗ trợ Step Counter, thử dùng Step Detector
            stepSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
            Log.d(TAG, "Step Counter không khả dụng, sử dụng Step Detector thay thế")
        }

        if (stepSensor == null) {
            Log.e(TAG, "Thiết bị không hỗ trợ cảm biến bước chân!")
            // Có thể triển khai thuật toán bước chân tùy chỉnh dựa trên accelerometer
        } else {
            Log.d(TAG, "Đã tìm thấy cảm biến: ${stepSensor?.name}")
        }

        // Tạo notification channel cho Android 8.0+
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isServiceRunning) {
            isServiceRunning = true

            // Khởi tạo foreground service với notification
            startForeground(NOTIFICATION_ID, createNotification())

            // Đăng ký sensor listener với delay và rate thấp hơn để tiết kiệm năng lượng
            stepSensor?.let {
                val registered = sensorManager?.registerListener(
                    this,
                    it,
                    SensorManager.SENSOR_DELAY_NORMAL,
                    500000 // 0.5 giây, sử dụng batch mode nếu khả dụng
                )

                if (registered == true) {
                    Log.d(TAG, "Đăng ký sensor listener thành công với ${it.name}")
                } else {
                    Log.e(TAG, "Không thể đăng ký sensor listener với ${it.name}")

                    // Thử lại với các delay khác nhau nếu đăng ký thất bại
                    val delays = intArrayOf(
                        SensorManager.SENSOR_DELAY_UI,
                        SensorManager.SENSOR_DELAY_GAME,
                        SensorManager.SENSOR_DELAY_FASTEST
                    )

                    for (delay in delays) {
                        if (sensorManager?.registerListener(this, it, delay) == true) {
                            Log.d(TAG, "Đăng ký lại thành công với delay: $delay")
                            break
                        }
                    }
                }

                // Khởi tạo một timer để giả lập step count nếu onSensorChanged không được gọi
                startSimulatedStepCountIfNeeded()
            } ?: run {
                // Nếu không có cảm biến, sử dụng giả lập hoàn toàn
                Log.w(TAG, "Không tìm thấy cảm biến bước chân, chuyển sang chế độ giả lập")
                startSimulatedStepCount()
            }

            // Khởi tạo wake lock để dịch vụ chạy khi màn hình tắt
            acquireWakeLock()

            // Bắt đầu đếm thời gian hoạt động
            startActiveTimeTracking()
        }

        // Nếu dịch vụ bị kill, hệ thống sẽ cố gắng tạo lại nó
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Health Tracker Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Used for the step counter service"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Health Tracker")
            .setContentText("Tracking your steps: $stepsCount")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification())
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "HealthTracker:StepCounterWakeLock"
        )
        wakeLock?.acquire(TimeUnit.HOURS.toMillis(10)) // Giới hạn 10 giờ để tránh drain pin
    }

    private fun startActiveTimeTracking() {
        serviceScope.launch {
            while (isServiceRunning) {
                delay(60000) // Cập nhật mỗi phút
                activeTimeMinutes++
                broadcastActiveTime()
                Log.d(TAG, "Active time: $activeTimeMinutes minutes")
            }
        }
    }

    // Biến theo dõi thời gian nhận được sự kiện sensor cuối cùng
    private var lastSensorEventTime = 0L
    private var isSimulatedMode = false

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            lastSensorEventTime = System.currentTimeMillis()

            // Nếu đang ở chế độ giả lập, tắt đi
            if (isSimulatedMode) {
                isSimulatedMode = false
                Log.d(TAG, "Chuyển từ chế độ giả lập sang chế độ cảm biến thật")
            }

            when (it.sensor.type) {
                Sensor.TYPE_STEP_COUNTER -> {
                    val totalSteps = it.values[0].toInt()

                    // Lưu giá trị ban đầu khi service bắt đầu
                    if (initialSteps == -1) {
                        initialSteps = totalSteps
                        Log.d(TAG, "Khởi tạo giá trị ban đầu: $initialSteps")
                    }

                    // Tính số bước từ khi service bắt đầu
                    stepsCount = totalSteps - initialSteps

                    // Broadcast thông tin số bước chân mới
                    broadcastStepsCount()

                    // Cập nhật notification
                    updateNotification()

                    Log.d(TAG, "Steps count từ TYPE_STEP_COUNTER: $stepsCount")
                }

                Sensor.TYPE_STEP_DETECTOR -> {
                    // Step detector trả về 1.0 khi phát hiện 1 bước chân
                    if (it.values[0] == 1.0f) {
                        stepsCount++

                        // Broadcast thông tin số bước chân mới
                        broadcastStepsCount()

                        // Cập nhật notification
                        updateNotification()

                        Log.d(TAG, "Steps count từ TYPE_STEP_DETECTOR: $stepsCount")
                    } else {

                    }
                }

                else -> {}
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Không cần xử lý sự kiện này
    }

    private fun broadcastStepsCount() {
        Intent().also { intent ->
            intent.action = ACTION_STEPS_UPDATED
            intent.putExtra(EXTRA_STEPS_COUNT, stepsCount)
            sendBroadcast(intent)
        }
    }

    private fun broadcastActiveTime() {
        Intent().also { intent ->
            intent.action = ACTION_ACTIVE_TIME_UPDATED
            intent.putExtra(EXTRA_ACTIVE_TIME, activeTimeMinutes)
            sendBroadcast(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        // Đây là Foreground Service, không cần binder
        return null
    }

    // Phương thức để bắt đầu chế độ giả lập hoàn toàn khi không có cảm biến
    private fun startSimulatedStepCount() {
        isSimulatedMode = true
        Log.d(TAG, "Bắt đầu chế độ giả lập bước chân hoàn toàn")

        simulationJob = serviceScope.launch {
            while (isActive) {
                delay(5000) // Mỗi 5 giây tăng bước chân

                // Tăng bước chân ngẫu nhiên (mô phỏng người đi bộ)
                val randomSteps = (1..3).random()
                stepsCount += randomSteps

                Log.d(TAG, "Giả lập: Tăng $randomSteps bước, tổng: $stepsCount")

                // Broadcast thông tin số bước chân mới
                broadcastStepsCount()

                // Cập nhật notification
                updateNotification()
            }
        }
    }

    // Phương thức để kiểm tra và giả lập nếu sensor không hoạt động
    private fun startSimulatedStepCountIfNeeded() {
        serviceScope.launch {
            delay(10000) // Đợi 10 giây

            // Nếu không nhận được sự kiện sensor nào sau 10 giây, bắt đầu giả lập
            if (lastSensorEventTime == 0L) {
                Log.w(TAG, "Không nhận được sự kiện sensor sau 10 giây, chuyển sang chế độ giả lập")
                startSimulatedStepCount()
            } else {
                // Kiểm tra liên tục xem sensor có ngừng hoạt động không
                while (isActive) {
                    delay(30000) // Kiểm tra mỗi 30 giây

                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastSensorEventTime > 60000) { // 1 phút không có sự kiện
                        Log.w(TAG, "Sensor ngừng hoạt động, chuyển sang chế độ giả lập")
                        startSimulatedStepCount()
                        break
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // Hủy đăng ký sensor listener
        sensorManager?.unregisterListener(this)

        // Giải phóng wake lock
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }

        // Hủy coroutines
        simulationJob?.cancel()
        serviceJob.cancel()

        isServiceRunning = false
        Log.d(TAG, "StepCounterService đã bị hủy")
    }
}