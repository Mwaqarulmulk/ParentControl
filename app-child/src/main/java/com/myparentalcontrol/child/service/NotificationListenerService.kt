package com.myparentalcontrol.child.service

import android.app.Notification
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.myparentalcontrol.child.util.DateTimeUtils
import com.myparentalcontrol.child.util.DeviceUtils
import com.myparentalcontrol.shared.ParentalControlApp
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * FEATURE 4: NOTIFICATION ACCESS
 * 
 * Captures all app notifications and forwards them to parent via Firebase
 * - Notification title, text, app name
 * - Timestamp
 * - Real-time syncing
 */
@AndroidEntryPoint
class NotificationListenerService : NotificationListenerService() {

    companion object {
        private const val TAG = "NotificationListener"
        
        // Apps to ignore (system apps that send too many notifications)
        private val IGNORED_PACKAGES = setOf(
            "android",
            "com.android.systemui",
            "com.google.android.gms",
            "com.myparentalcontrol.child" // Don't capture our own notifications
        )
    }

    @Inject
    lateinit var firestore: FirebaseFirestore

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val realtimeDb = FirebaseDatabase.getInstance()
    private var deviceId: String = ""

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Notification Listener Service created")
        deviceId = DeviceUtils.getDeviceId(this)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)
        
        val packageName = sbn.packageName
        
        // Ignore system and own notifications
        if (IGNORED_PACKAGES.contains(packageName)) {
            return
        }

        val notification = sbn.notification ?: return
        
        try {
            val notificationData = extractNotificationData(sbn, notification)
            saveNotificationToFirebase(notificationData)
            
            Log.d(TAG, "Notification captured from $packageName: ${notificationData["title"]}")
        } catch (e: Exception) {
            Log.e(TAG, "Error processing notification", e)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        super.onNotificationRemoved(sbn)
        // Optionally track notification dismissal
    }

    /**
     * Extract notification data
     */
    private fun extractNotificationData(
        sbn: StatusBarNotification,
        notification: Notification
    ): Map<String, Any?> {
        val extras = notification.extras
        
        val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras?.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: text
        val subText = extras?.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: ""
        
        // Get app name
        val appName = try {
            val pm = packageManager
            val appInfo = pm.getApplicationInfo(sbn.packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            sbn.packageName
        }
        
        return hashMapOf(
            "deviceId" to deviceId,
            "packageName" to sbn.packageName,
            "appName" to appName,
            "title" to title,
            "text" to text,
            "bigText" to bigText,
            "subText" to subText,
            "timestamp" to DateTimeUtils.getCurrentTimestamp(),
            "postTime" to sbn.postTime,
            "notificationId" to sbn.id,
            "tag" to (sbn.tag ?: ""),
            "category" to (notification.category ?: ""),
            "priority" to notification.priority,
            "isOngoing" to ((notification.flags and Notification.FLAG_ONGOING_EVENT) != 0),
            "isClearable" to sbn.isClearable,
            "isGroup" to (notification.group != null)
        )
    }

    /**
     * Save notification to Firebase (Realtime DB for instant sync + Firestore for history)
     */
    private fun saveNotificationToFirebase(notificationData: Map<String, Any?>) {
        scope.launch {
            try {
                val notificationId = "${System.currentTimeMillis()}_${notificationData["notificationId"]}"
                
                // 1. Save to Realtime Database for INSTANT sync to parent
                // Latest notification (always overwritten)
                realtimeDb.getReference("notifications/$deviceId/latest")
                    .setValue(notificationData)
                
                // Notification history in Realtime DB (for real-time list updates)
                realtimeDb.getReference("notifications/$deviceId/history/$notificationId")
                    .setValue(notificationData)
                
                // Update unread count
                realtimeDb.getReference("notifications/$deviceId/unreadCount")
                    .get().addOnSuccessListener { snapshot ->
                        val currentCount = snapshot.getValue(Int::class.java) ?: 0
                        realtimeDb.getReference("notifications/$deviceId/unreadCount")
                            .setValue(currentCount + 1)
                    }
                
                // 2. Save to Firestore for persistent history
                firestore.collection("notifications")
                    .document(deviceId)
                    .collection("history")
                    .add(notificationData)
                    .await()

                // Update latest notification in device document
                firestore.collection(ParentalControlApp.FirebaseCollections.DEVICES)
                    .document(deviceId)
                    .update(
                        mapOf(
                            "lastNotification" to notificationData,
                            "lastNotificationTime" to DateTimeUtils.getCurrentTimestamp()
                        )
                    )
                    .await()

                Log.d(TAG, "Notification saved to Firebase (Realtime + Firestore)")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save notification to Firebase", e)
            }
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "Notification Listener connected")
        
        // Optionally: Send event to parent that notification access is enabled
        scope.launch {
            try {
                firestore.collection(ParentalControlApp.FirebaseCollections.DEVICES)
                    .document(deviceId)
                    .update("notificationAccessEnabled", true)
                    .await()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update notification access status", e)
            }
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d(TAG, "Notification Listener disconnected")
        
        // Notify parent that notification access was disabled
        scope.launch {
            try {
                firestore.collection(ParentalControlApp.FirebaseCollections.DEVICES)
                    .document(deviceId)
                    .update("notificationAccessEnabled", false)
                    .await()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update notification access status", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Notification Listener Service destroyed")
    }
}
