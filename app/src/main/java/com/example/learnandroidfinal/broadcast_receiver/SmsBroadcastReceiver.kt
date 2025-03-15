package com.example.learnandroidfinal.broadcast_receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log

class SmsBroadcastReceiver(private val onSmsReceived: (String) -> Unit) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)

            val builder = StringBuilder()
            messages.forEach { smsMessage ->
                val phone = smsMessage.displayOriginatingAddress
                val body = smsMessage.displayMessageBody

                builder.append("Từ: $phone\n")
                builder.append("Nội dung: $body\n\n")

                Log.d("SMSReceiver", "Nhận SMS từ: $phone - Nội dung: $body")
            }

            if (builder.isNotEmpty()) {
                onSmsReceived(builder.toString())
            }
        }
    }
}
