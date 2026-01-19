package com.myparentalcontrol.child.service

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
import com.myparentalcontrol.child.MainActivity
import com.myparentalcontrol.child.R

class ChildFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "ChildFCMService"
        private const val CHANNEL_ID = "child_notifications"
        private const val CHANNEL_NAME = "Child Notifications"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token: $token")
        // TODO: Send token to server for this child device
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
            "start_stream" -> {
                // Parent requested to start streaming
                val streamType = data["streamType"]
                Log.d(TAG, "Start stream request: $streamType")
                // TODO: Start appropriate streaming service
            }
            "stop_stream" -> {
                // Parent requested to stop streaming
                Log.d(TAG, "Stop stream request")
                // TODO: Stop streaming services
            }
            "block_app" -> {
                // Parent requested to block an app
                val packageName = data["packageName"]
                packageName?.let {
                    AppBlockingAccessibilityService.addBlockedApp(it)
                    Log.d(TAG, "App blocked: $packageName")
                }
            }
            "unblock_app" -> {
                // Parent requested to unblock an app
                val packageName = data["packageName"]
                packageName?.let {
                    AppBlockingAccessibilityService.removeBlockedApp(it)
                    Log.d(TAG, "App unblocked: $packageName")
                }
            }
            "update_settings" -> {
                // Parent updated settings
                Log.d(TAG, "Settings update received")
                // TODO: Update local settings
            }
            "request_location" -> {
                // Parent requested current location
                Log.d(TAG, "Location request received")
                // TODO: Send current location
            }
            "lock_device" -> {
                // Parent requested to lock device
                Log.d(TAG, "Lock device request")
                // TODO: Lock device (requires device admin)
            }
            "play_sound" -> {
                // Parent requested to play sound (find device)
                Log.d(TAG, "Play sound request")
                // TODO: Play loud sound
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
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
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
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications from parent"
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
