package com.example.learnandroidfinal

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.learnandroidfinal.databinding.ActivityMainBinding
import com.example.learnandroidfinal.service.BoundService
import com.example.learnandroidfinal.service.MyForegroundService

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private var bound = false
    private var boundService: BoundService? = null

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            bound = true
            boundService = (service as BoundService.LocalBinder).getService()

            Toast.makeText(
                this@MainActivity,
                boundService?.getDemoData(),
                Toast.LENGTH_SHORT
            ).show()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            bound = false
            boundService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        handlerStartForegroundService()
        handlerStopForegroundService()
    }

    override fun onStart() {
        super.onStart()
        this.bindService(
            Intent(this, BoundService::class.java),
            connection,
            BIND_AUTO_CREATE
        )
    }

    override fun onStop() {
        super.onStop()
        if (bound) {
            unbindService(connection)
            bound = false
        }
    }

    private fun handlerStartForegroundService() {
        binding.btnForegroundService.setOnClickListener {
            ContextCompat.startForegroundService(
                this,
                Intent(this, MyForegroundService::class.java).apply {
                    putExtra(MyForegroundService.ACTION_EXTRA_KEY, "Start")
                }
            )
        }
    }

    private fun handlerStopForegroundService() {
        binding.btnStopForegroundService.setOnClickListener {
            stopService(Intent(this, MyForegroundService::class.java))
        }
    }
}