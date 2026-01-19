package com.myparentalcontrol.parent.service

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
import com.myparentalcontrol.parent.MainActivity
import com.myparentalcontrol.parent.R

class WebRTCConnectionService : Service() {

    companion object {
        private const val TAG = "WebRTCConnectionService"
        private const val NOTIFICATION_ID = 2001
        private const val CHANNEL_ID = "webrtc_connection_channel"
        private const val CHANNEL_NAME = "WebRTC Connection"

        const val ACTION_START = "com.myparentalcontrol.parent.action.START_WEBRTC"
        const val ACTION_STOP = "com.myparentalcontrol.parent.action.STOP_WEBRTC"
        
        const val EXTRA_CHILD_ID = "extra_child_id"
        const val EXTRA_CHILD_NAME = "extra_child_name"

        fun startService(context: Context, childId: String, childName: String) {
            val intent = Intent(context, WebRTCConnectionService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_CHILD_ID, childId)
                putExtra(EXTRA_CHILD_NAME, childName)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, WebRTCConnectionService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private var childId: String? = null
    private var childName: String? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                childId = intent.getStringExtra(EXTRA_CHILD_ID)
                childName = intent.getStringExtra(EXTRA_CHILD_NAME)
                startForeground(NOTIFICATION_ID, createNotification())
                Log.d(TAG, "Started WebRTC connection for child: $childName ($childId)")
            }
            ACTION_STOP -> {
                Log.d(TAG, "Stopping WebRTC connection")
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
                description = "WebRTC streaming connection"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, WebRTCConnectionService::class.java).apply {
            action = ACTION_STOP
        }
        
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Viewing Stream")
            .setContentText("Connected to ${childName ?: "child device"}")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Disconnect",
                stopPendingIntent
            )
            .build()
    }
}
