package com.myparentalcontrol.child.util

import android.content.Context
import android.content.Intent
import android.provider.Settings

/**
 * Simplified permission checker for 5 core features
 */
object FiveFeaturePermissions {
    
    /**
     * Check Feature 1-3: Camera, Screen Mirror, Audio (WebRTC Streaming)
     */
    fun hasStreamingPermissions(context: Context): Boolean {
        return PermissionUtils.hasAllDangerousPermissions(context) // Includes CAMERA and RECORD_AUDIO
    }
    
    /**
     * Check Feature 4: Notification Access
     */
    fun hasNotificationAccess(context: Context): Boolean {
        val packageName = context.packageName
        val flat = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        )
        return flat != null && flat.contains(packageName)
    }
    
    /**
     * Check Feature 5: Location Tracking
     */
    fun hasLocationPermissions(context: Context): Boolean {
        return PermissionUtils.hasAllDangerousPermissions(context) && 
               PermissionUtils.hasBackgroundLocationPermission(context)
    }
    
    /**
     * Check if all 5 core features have permissions
     */
    fun hasAllCorePermissions(context: Context): Boolean {
        return hasStreamingPermissions(context) &&
               hasNotificationAccess(context) &&
               hasLocationPermissions(context) &&
               PermissionUtils.isBatteryOptimizationDisabled(context)
    }
    
    /**
     * Get list of missing permissions for UI display
     */
    fun getMissingPermissions(context: Context): List<String> {
        val missing = mutableListOf<String>()
        
        if (!hasStreamingPermissions(context)) {
            missing.add("Camera & Microphone (for streaming)")
        }
        
        if (!hasNotificationAccess(context)) {
            missing.add("Notification Access")
        }
        
        if (!hasLocationPermissions(context)) {
            missing.add("Location (including background)")
        }
        
        if (!PermissionUtils.isBatteryOptimizationDisabled(context)) {
            missing.add("Battery Optimization (must be disabled)")
        }
        
        return missing
    }
    
    /**
     * Open notification access settings
     */
    fun openNotificationAccessSettings(context: Context) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        context.startActivity(intent)
    }
}
