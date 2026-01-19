package com.myparentalcontrol.parent

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

/**
 * Parent app Application class with Hilt support
 */
@HiltAndroidApp
class ParentApplication : Application() {
    
    companion object {
        const val NOTIFICATION_CHANNEL_ALERTS = "alerts_channel"
        const val NOTIFICATION_CHANNEL_STREAMING = "streaming_channel"
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }
    
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)
            
            // Alerts channel for important notifications
            val alertsChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ALERTS,
                "Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Important alerts from child device"
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(alertsChannel)
            
            // Streaming channel for stream notifications
            val streamingChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_STREAMING,
                "Streaming",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Active streaming notifications"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(streamingChannel)
        }
    }
}
