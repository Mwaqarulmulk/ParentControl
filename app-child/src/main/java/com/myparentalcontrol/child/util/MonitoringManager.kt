package com.myparentalcontrol.child.util

import android.content.Context
import android.util.Log
import com.myparentalcontrol.child.service.LocationTrackingService
import com.myparentalcontrol.child.service.MonitoringService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages all monitoring services in the child app
 * Provides a centralized way to start/stop services
 */
@Singleton
class MonitoringManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val permissionManager: PermissionManager
) {
    
    companion object {
        private const val TAG = "MonitoringManager"
        private const val PREFS_NAME = "monitoring_prefs"
        private const val KEY_SERVICES_STARTED = "services_started"
    }
    
    /**
     * Data class for service status
     */
    data class ServiceStatus(
        val name: String,
        val description: String,
        val isRunning: Boolean,
        val permissionGranted: Boolean
    )
    
    /**
     * Start all monitoring services
     * Should be called after pairing is complete
     */
    fun startAllServices() {
        Log.d(TAG, "Starting all monitoring services")
        
        // Start main monitoring service
        try {
            MonitoringService.startService(context)
            Log.d(TAG, "MonitoringService started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start MonitoringService", e)
        }
        
        // Start location tracking if permission granted
        if (permissionManager.isLocationPermissionGranted(context)) {
            try {
                LocationTrackingService.startService(context)
                Log.d(TAG, "LocationTrackingService started")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start LocationTrackingService", e)
            }
        } else {
            Log.w(TAG, "Location permission not granted, skipping LocationTrackingService")
        }
        
        // Mark services as started
        saveServicesStarted(true)
    }
    
    /**
     * Stop all monitoring services
     */
    fun stopAllServices() {
        Log.d(TAG, "Stopping all monitoring services")
        
        try {
            MonitoringService.stopService(context)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop MonitoringService", e)
        }
        
        try {
            LocationTrackingService.stopService(context)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop LocationTrackingService", e)
        }
        
        saveServicesStarted(false)
    }
    
    /**
     * Get status of all services
     */
    fun getServicesStatus(): List<ServiceStatus> {
        val cameraPermission = permissionManager.isCameraPermissionGranted(context)
        val audioPermission = permissionManager.isAudioPermissionGranted(context)
        val locationPermission = permissionManager.isLocationPermissionGranted(context)
        val notificationListener = permissionManager.isNotificationListenerEnabled(context)
        val servicesStarted = areServicesStarted()
        
        return listOf(
            ServiceStatus(
                name = "Camera Streaming",
                description = "Stream camera to parent device",
                isRunning = servicesStarted && cameraPermission,
                permissionGranted = cameraPermission
            ),
            ServiceStatus(
                name = "Screen Mirroring",
                description = "Mirror screen to parent device",
                isRunning = servicesStarted, // Screen mirroring requires additional setup
                permissionGranted = true // Requires media projection
            ),
            ServiceStatus(
                name = "Audio Streaming",
                description = "Stream microphone to parent device",
                isRunning = servicesStarted && audioPermission,
                permissionGranted = audioPermission
            ),
            ServiceStatus(
                name = "Notification Access",
                description = "Monitor app notifications",
                isRunning = notificationListener,
                permissionGranted = notificationListener
            ),
            ServiceStatus(
                name = "Location Tracking",
                description = "Track device location",
                isRunning = servicesStarted && locationPermission,
                permissionGranted = locationPermission
            )
        )
    }
    
    /**
     * Check if services have been started
     */
    fun areServicesStarted(): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_SERVICES_STARTED, false)
    }
    
    /**
     * Save services started state
     */
    private fun saveServicesStarted(started: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_SERVICES_STARTED, started).apply()
    }
    
    /**
     * Restart services that may have been killed
     */
    fun ensureServicesRunning() {
        if (areServicesStarted()) {
            Log.d(TAG, "Ensuring services are running")
            startAllServices()
        }
    }
}
