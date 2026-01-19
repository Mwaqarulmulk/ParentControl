package com.myparentalcontrol.parent.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.myparentalcontrol.parent.MainActivity
import com.myparentalcontrol.parent.R

class ParentFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "ParentFCMService"
        private const val CHANNEL_ID = "parental_control_notifications"
        private const val CHANNEL_NAME = "Parental Control Notifications"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token: $token")
        // TODO: Send token to server for this parent device
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "Message received from: ${remoteMessage.from}")

        // Handle data payload
        remoteMessage.data.isNotEmpty().let {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
            handleDataMessage(remoteMessage.data)
        }

        // Handle notification payload
        remoteMessage.notification?.let {
            Log.d(TAG, "Message notification body: ${it.body}")
            showNotification(it.title ?: "Parental Control", it.body ?: "")
        }
    }

    private fun handleDataMessage(data: Map<String, String>) {
        val type = data["type"] ?: return
        
        when (type) {
            "child_location_update" -> {
                // Handle location update from child
                val childId = data["childId"]
                val latitude = data["latitude"]?.toDoubleOrNull()
                val longitude = data["longitude"]?.toDoubleOrNull()
                Log.d(TAG, "Child $childId location: $latitude, $longitude")
            }
            "child_app_usage" -> {
                // Handle app usage update
                val childId = data["childId"]
                val appName = data["appName"]
                val duration = data["duration"]
                Log.d(TAG, "Child $childId used $appName for $duration")
            }
            "child_alert" -> {
                // Handle alert from child device
                val childId = data["childId"]
                val alertType = data["alertType"]
                val message = data["message"] ?: "Alert from child device"
                showNotification("Child Alert", message)
            }
            "child_online" -> {
                // Child device came online
                val childId = data["childId"]
                val childName = data["childName"] ?: "Child"
                showNotification("Device Online", "$childName's device is now online")
            }
            "child_offline" -> {
                // Child device went offline
                val childId = data["childId"]
                val childName = data["childName"] ?: "Child"
                showNotification("Device Offline", "$childName's device went offline")
            }
            "screen_time_exceeded" -> {
                // Screen time limit exceeded
                val childId = data["childId"]
                val childName = data["childName"] ?: "Child"
                showNotification("Screen Time Alert", "$childName has exceeded their screen time limit")
            }
            "geofence_exit" -> {
                // Child left geofence area
                val childId = data["childId"]
                val childName = data["childName"] ?: "Child"
                val zoneName = data["zoneName"] ?: "safe zone"
                showNotification("Location Alert", "$childName has left $zoneName")
            }
            "geofence_enter" -> {
                // Child entered geofence area
                val childId = data["childId"]
                val childName = data["childName"] ?: "Child"
                val zoneName = data["zoneName"] ?: "safe zone"
                showNotification("Location Alert", "$childName has arrived at $zoneName")
            }
            else -> {
                Log.d(TAG, "Unknown message type: $type")
            }
        }
    }

    private fun showNotification(title: String, message: String) {
        createNotificationChannel()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications from child devices"
                enableVibration(true)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
