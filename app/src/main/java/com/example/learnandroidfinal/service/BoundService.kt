package com.example.learnandroidfinal.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder

class BoundService : Service() {

    inner class LocalBinder : Binder() {
        fun getService(): BoundService {
            return this@BoundService
        }
    }

    private val localBinder = LocalBinder()

    override fun onBind(intent: Intent?): IBinder {
        log("onBind intent: $intent")
        return localBinder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        log("onUnbind intent: $intent")
        return super.onUnbind(intent)
    }

    override fun onRebind(intent: Intent?) {
        super.onRebind(intent)
        log("onRebind intent: $intent")
    }

    fun getDemoData(): String {
        return "Demo data from BoundService"
    }

    private fun log (message: String) {
        println("$TAG: $message")
    }

    companion object {
        const val TAG = "BoundService"
    }
}
