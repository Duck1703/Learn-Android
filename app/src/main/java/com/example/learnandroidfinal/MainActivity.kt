package com.example.learnandroidfinal

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.learnandroidfinal.databinding.ActivityMainBinding
import com.example.learnandroidfinal.service.MyForegroundService

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        handlerStartForegroundService()
        handlerStopForegroundService()
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