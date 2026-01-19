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
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.myparentalcontrol.child.MainActivity
import com.myparentalcontrol.child.R
import com.myparentalcontrol.child.util.DeviceUtils
import com.myparentalcontrol.shared.ParentalControlApp
import com.myparentalcontrol.shared.data.supabase.SupabaseRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Main monitoring service that coordinates all child device monitoring activities.
 * This service:
 * - Starts/stops other specialized services (location, command listener, etc.)
 * - Periodically syncs device status to Firebase AND Supabase
 * - Manages battery and network status reporting
 */
@AndroidEntryPoint
class MonitoringService : Service() {

    companion object {
        private const val TAG = "MonitoringService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "monitoring_channel"
        private const val CHANNEL_NAME = "Monitoring Service"
        private const val STATUS_SYNC_INTERVAL = 60_000L // 1 minute

        const val ACTION_START = "com.myparentalcontrol.child.action.START_MONITORING"
        const val ACTION_STOP = "com.myparentalcontrol.child.action.STOP_MONITORING"

        fun startService(context: Context) {
            val intent = Intent(context, MonitoringService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, MonitoringService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    @Inject
    lateinit var firestore: FirebaseFirestore
    
    @Inject
    lateinit var supabaseRepository: SupabaseRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val realtimeDb = FirebaseDatabase.getInstance()
    private var statusSyncJob: Job? = null
    private var deviceId: String = ""

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        createNotificationChannel()
        deviceId = DeviceUtils.getDeviceId(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForeground(NOTIFICATION_ID, createNotification())
                Log.d(TAG, "Monitoring started for device: $deviceId")
                startAllServices()
                startStatusSync()
            }
            ACTION_STOP -> {
                Log.d(TAG, "Stopping monitoring")
                stopAllServices()
                stopStatusSync()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        updateOnlineStatus(false)
        Log.d(TAG, "Service destroyed")
    }

    private fun startAllServices() {
        Log.d(TAG, "Starting all monitoring services")
        
        // Start command listener to receive parent commands
        CommandListenerService.startService(this)
        
        // Start location tracking
        LocationTrackingService.startService(this)
        
        // Start streaming service (ready for WebRTC connections)
        com.myparentalcontrol.child.streaming.services.StreamingService.startService(this, deviceId)
        
        // Mark device as online
        updateOnlineStatus(true)
    }

    private fun stopAllServices() {
        Log.d(TAG, "Stopping all monitoring services")
        
        CommandListenerService.stopService(this)
        LocationTrackingService.stopService(this)
        com.myparentalcontrol.child.streaming.services.StreamingService.stopService(this)
        CameraStreamingService.stopService(this)
        ScreenMirroringService.stopService(this)
        AudioStreamingService.stopService(this)
    }

    private fun startStatusSync() {
        statusSyncJob?.cancel()
        statusSyncJob = scope.launch {
            while (isActive) {
                syncDeviceStatus()
                delay(STATUS_SYNC_INTERVAL)
            }
        }
    }

    private fun stopStatusSync() {
        statusSyncJob?.cancel()
        statusSyncJob = null
    }

    private suspend fun syncDeviceStatus() {
        try {
            val batteryInfo = DeviceUtils.getBatteryInfo(this@MonitoringService)
            val networkType = DeviceUtils.getNetworkType(this@MonitoringService)
            
            val statusUpdate = hashMapOf<String, Any>(
                "isOnline" to true,
                "lastSeen" to System.currentTimeMillis(),
                "batteryLevel" to batteryInfo.level,
                "isCharging" to batteryInfo.isCharging,
                "networkType" to networkType
            )
            
            // 1. Sync to Realtime Database for INSTANT updates
            realtimeDb.getReference("devices/$deviceId/status")
                .setValue(statusUpdate)
            
            // 2. Sync to Firestore for persistence
            firestore.collection(ParentalControlApp.FirebaseCollections.DEVICES)
                .document(deviceId)
                .update(statusUpdate)
                .await()
            
            // 3. Sync to Supabase for cross-platform support
            supabaseRepository.updateDeviceStatus(
                deviceId = deviceId,
                isOnline = true,
                batteryLevel = batteryInfo.level,
                isCharging = batteryInfo.isCharging,
                networkType = networkType
            )

            Log.d(TAG, "Device status synced to Firebase + Supabase: battery=${batteryInfo.level}%, network=$networkType")
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing device status", e)
        }
    }

    private fun updateOnlineStatus(isOnline: Boolean) {
        scope.launch {
            try {
                val statusUpdate = mapOf(
                    "isOnline" to isOnline,
                    "lastSeen" to System.currentTimeMillis()
                )
                
                // 1. Update Realtime Database for INSTANT updates
                realtimeDb.getReference("devices/$deviceId/status")
                    .updateChildren(statusUpdate)
                
                // 2. Update Firestore
                firestore.collection(ParentalControlApp.FirebaseCollections.DEVICES)
                    .document(deviceId)
                    .update(statusUpdate)
                    .await()
                
                // 3. Update Supabase
                supabaseRepository.updateDeviceStatus(
                    deviceId = deviceId,
                    isOnline = isOnline
                )
                    
                Log.d(TAG, "Online status updated to Firebase + Supabase: $isOnline")
            } catch (e: Exception) {
                Log.e(TAG, "Error updating online status", e)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Parental control monitoring service"
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

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Parental Control Active")
            .setContentText("Device is being monitored")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }
}
