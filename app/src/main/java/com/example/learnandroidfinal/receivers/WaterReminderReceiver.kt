package com.example.learnandroidfinal.receivers

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.learnandroidfinal.MainActivity
import com.example.learnandroidfinal.R

class WaterReminderReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "WaterReminderReceiver"
        private const val NOTIFICATION_ID = 2001
        private const val CHANNEL_ID = "water_reminder_channel"

        // Action để xác định intent từ AlarmManager
        const val ACTION_WATER_REMINDER = "com.example.healthtracker.WATER_REMINDER"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_WATER_REMINDER) {
            Log.d(TAG, "Water reminder alarm received")

            // Hiển thị notification nhắc nhở uống nước
            showWaterReminderNotification(context)
        }
    }

    private fun showWaterReminderNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Tạo channel cho notification (yêu cầu từ Android 8.0+)
        createNotificationChannel(notificationManager)

        // Intent để mở MainActivity khi user nhấn vào notification
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        // Tạo notification
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Uống nước nào!")
            .setContentText("Đã đến lúc uống nước rồi đấy, giữ sức khỏe nào!")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        // Hiển thị notification
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun createNotificationChannel(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Water Reminder",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminders to drink water"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
}