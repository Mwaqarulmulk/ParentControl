package com.myparentalcontrol.child

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.myparentalcontrol.shared.ParentalControlApp
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ChildApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            // Monitoring Service Channel
            val monitoringChannel = NotificationChannel(
                ParentalControlApp.NotificationChannels.MONITORING,
                getString(R.string.channel_monitoring_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.channel_monitoring_description)
                setShowBadge(false)
            }

            // Alerts Channel
            val alertsChannel = NotificationChannel(
                ParentalControlApp.NotificationChannels.ALERTS,
                getString(R.string.channel_alerts_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.channel_alerts_description)
                enableVibration(true)
            }

            // Streaming Channel
            val streamingChannel = NotificationChannel(
                ParentalControlApp.NotificationChannels.STREAMING,
                "Streaming",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when streaming is active"
                setShowBadge(false)
            }

            notificationManager.createNotificationChannels(
                listOf(monitoringChannel, alertsChannel, streamingChannel)
            )
        }
    }
}
