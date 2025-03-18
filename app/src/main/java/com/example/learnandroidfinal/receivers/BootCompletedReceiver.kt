package com.example.learnandroidfinal.receivers

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.learnandroidfinal.service.StepCounterService

class BootCompletedReceiver : BroadcastReceiver() {

    @SuppressLint("ObsoleteSdkInt")
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Boot completed, starting StepCounterService")

            val serviceIntent = Intent(context, StepCounterService::class.java)

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context?.startForegroundService(serviceIntent)
            } else {
                context?.startService(serviceIntent)
            }
        }
    }

    companion object {
        private const val TAG = "BootCompletedReceiver"
    }
}