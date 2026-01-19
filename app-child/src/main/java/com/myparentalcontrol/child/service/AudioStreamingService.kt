package com.myparentalcontrol.child.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.myparentalcontrol.child.MainActivity
import com.myparentalcontrol.child.R
import com.myparentalcontrol.child.streaming.services.StreamingService
import com.myparentalcontrol.child.util.DeviceUtils

/**
 * Audio Streaming Service - Acts as a bridge to the main StreamingService
 * This service is triggered by commands from CommandListenerService
 * and delegates actual audio streaming to StreamingService which handles
 * microphone capture + WebRTC for one-way audio (child to parent)
 */
class AudioStreamingService : Service() {

    companion object {
        private const val TAG = "AudioStreamingService"
        private const val NOTIFICATION_ID = 1004
        private const val CHANNEL_ID = "audio_streaming_channel"
        private const val CHANNEL_NAME = "Audio Streaming"

        const val ACTION_START = "com.myparentalcontrol.child.action.START_AUDIO_STREAM"
        const val ACTION_STOP = "com.myparentalcontrol.child.action.STOP_AUDIO_STREAM"

        fun startService(context: Context) {
            val intent = Intent(context, AudioStreamingService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, AudioStreamingService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForeground(NOTIFICATION_ID, createNotification())
                Log.d(TAG, "Audio streaming started (one-way audio to parent)")
                
                // Start the main StreamingService which handles microphone capture + WebRTC
                val deviceId = DeviceUtils.getDeviceId(this)
                StreamingService.startService(this, deviceId)
            }
            ACTION_STOP -> {
                Log.d(TAG, "Stopping audio streaming")
                
                // Stop the StreamingService
                StreamingService.stopService(this)
                
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Audio streaming service"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Audio Active")
            .setContentText("One-way audio streaming is active")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }
}
