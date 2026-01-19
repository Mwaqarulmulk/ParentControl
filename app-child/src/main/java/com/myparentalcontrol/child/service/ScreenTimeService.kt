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
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.myparentalcontrol.child.MainActivity
import com.myparentalcontrol.child.R

class ScreenTimeService : Service() {

    companion object {
        private const val TAG = "ScreenTimeService"
        private const val NOTIFICATION_ID = 1007
        private const val CHANNEL_ID = "screen_time_channel"
        private const val CHANNEL_NAME = "Screen Time Monitoring"

        const val ACTION_START = "com.myparentalcontrol.child.action.START_SCREEN_TIME"
        const val ACTION_STOP = "com.myparentalcontrol.child.action.STOP_SCREEN_TIME"

        fun startService(context: Context) {
            val intent = Intent(context, ScreenTimeService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, ScreenTimeService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private var screenOnTime: Long = 0
    private var lastScreenOnTime: Long = 0

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForeground(NOTIFICATION_ID, createNotification())
                Log.d(TAG, "Screen time monitoring started")
                // TODO: Register screen state receiver
            }
            ACTION_STOP -> {
                Log.d(TAG, "Stopping screen time monitoring")
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
                description = "Screen time monitoring service"
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
            .setContentTitle("Screen Time Active")
            .setContentText("Screen time monitoring is active")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }
}
