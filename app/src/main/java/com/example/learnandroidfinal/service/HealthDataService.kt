package com.example.learnandroidfinal.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Binder
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HealthDataService : Service() {
    private val binder = HealthDataBinder()

    private val _stepsCount = MutableStateFlow(0)
    val stepsCount: StateFlow<Int> = _stepsCount.asStateFlow()

    private val _activeTimeMinutes = MutableStateFlow(0L)
    val activeTimeMinutes = _activeTimeMinutes.asStateFlow()

    private val dataReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                StepCounterService.ACTION_STEPS_UPDATED -> {
                    val steps = intent.getIntExtra(StepCounterService.EXTRA_STEPS_COUNT, 0)
                    _stepsCount.value = steps
                }

                StepCounterService.ACTION_ACTIVE_TIME_UPDATED -> {
                    val time = intent.getLongExtra(StepCounterService.EXTRA_ACTIVE_TIME, 0L)
                    _activeTimeMinutes.value = time
                }
            }
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate() {
        super.onCreate()
        val intentFilter = IntentFilter().apply {
            addAction(StepCounterService.ACTION_STEPS_UPDATED)
            addAction(StepCounterService.ACTION_ACTIVE_TIME_UPDATED)
        }
        registerReceiver(dataReceiver, intentFilter)
        Log.d(TAG, "HealthDataService created")
    }

    override fun onBind(p0: Intent?): IBinder {
        Log.d(TAG, "HealthDataService bound")
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(dataReceiver)
        Log.d(TAG, "HealthDataService destroyed")
    }

    fun getFormattedActiveTime(): String {
        val hours = _activeTimeMinutes.value / 60
        val minutes = _activeTimeMinutes.value % 60
        return "${hours}h ${minutes}m"
    }

    fun getCurrentSteps(): Int = _stepsCount.value

    inner class HealthDataBinder : Binder() {
        fun getService(): HealthDataService = this@HealthDataService
    }

    companion object {
        private const val TAG = "HealthDataService"
    }
}