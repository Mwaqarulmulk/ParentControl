package com.myparentalcontrol.child.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized permission manager for the child app
 * Handles all runtime permission checks and requests
 */
@Singleton
class PermissionManager @Inject constructor() {
    
    companion object {
        // Core permissions that need runtime approval
        val CAMERA_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.CAMERA)
        } else {
            arrayOf(Manifest.permission.CAMERA)
        }
        
        val AUDIO_PERMISSIONS = arrayOf(Manifest.permission.RECORD_AUDIO)
        
        val LOCATION_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }
        
        val BACKGROUND_LOCATION_PERMISSION = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else {
            emptyArray()
        }
        
        val NOTIFICATION_PERMISSION = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            emptyArray()
        }
        
        // All runtime permissions combined (except background location which needs separate request)
        val ALL_BASIC_PERMISSIONS: Array<String>
            get() = buildList {
                addAll(CAMERA_PERMISSIONS)
                addAll(AUDIO_PERMISSIONS)
                addAll(LOCATION_PERMISSIONS)
                addAll(NOTIFICATION_PERMISSION)
            }.toTypedArray()
    }
    
    /**
     * Data class representing a permission with its metadata
     */
    data class PermissionInfo(
        val permission: String,
        val name: String,
        val description: String,
        val isGranted: Boolean,
        val isRequired: Boolean = true
    )
    
    /**
     * Check if a specific permission is granted
     */
    fun isPermissionGranted(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == 
            PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Check if all basic permissions are granted
     */
    fun areAllBasicPermissionsGranted(context: Context): Boolean {
        return ALL_BASIC_PERMISSIONS.all { isPermissionGranted(context, it) }
    }
    
    /**
     * Check if camera permission is granted
     */
    fun isCameraPermissionGranted(context: Context): Boolean {
        return CAMERA_PERMISSIONS.all { isPermissionGranted(context, it) }
    }
    
    /**
     * Check if audio permission is granted
     */
    fun isAudioPermissionGranted(context: Context): Boolean {
        return AUDIO_PERMISSIONS.all { isPermissionGranted(context, it) }
    }
    
    /**
     * Check if location permissions are granted
     */
    fun isLocationPermissionGranted(context: Context): Boolean {
        return LOCATION_PERMISSIONS.all { isPermissionGranted(context, it) }
    }
    
    /**
     * Check if background location is granted (requires separate request on Android 10+)
     */
    fun isBackgroundLocationGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            isPermissionGranted(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else {
            true // Not needed on older versions
        }
    }
    
    /**
     * Check if notification permission is granted (Android 13+)
     */
    fun isNotificationPermissionGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            isPermissionGranted(context, Manifest.permission.POST_NOTIFICATIONS)
        } else {
            true // Not needed on older versions
        }
    }
    
    /**
     * Check if notification listener access is enabled
     */
    fun isNotificationListenerEnabled(context: Context): Boolean {
        val packageName = context.packageName
        val enabledListeners = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        )
        return enabledListeners?.contains(packageName) == true
    }
    
    /**
     * Check if battery optimization is disabled for this app
     */
    fun isBatteryOptimizationDisabled(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }
    
    /**
     * Get list of all permissions with their status
     */
    fun getPermissionStatus(context: Context): List<PermissionInfo> {
        return buildList {
            add(PermissionInfo(
                permission = Manifest.permission.CAMERA,
                name = "Camera",
                description = "Required for camera streaming to parent device",
                isGranted = isCameraPermissionGranted(context)
            ))
            
            add(PermissionInfo(
                permission = Manifest.permission.RECORD_AUDIO,
                name = "Microphone",
                description = "Required for audio streaming to parent device",
                isGranted = isAudioPermissionGranted(context)
            ))
            
            add(PermissionInfo(
                permission = Manifest.permission.ACCESS_FINE_LOCATION,
                name = "Location",
                description = "Required for location tracking",
                isGranted = isLocationPermissionGranted(context)
            ))
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(PermissionInfo(
                    permission = Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                    name = "Background Location",
                    description = "Required for continuous location tracking",
                    isGranted = isBackgroundLocationGranted(context)
                ))
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(PermissionInfo(
                    permission = Manifest.permission.POST_NOTIFICATIONS,
                    name = "Notifications",
                    description = "Required to show service notifications",
                    isGranted = isNotificationPermissionGranted(context)
                ))
            }
        }
    }
    
    /**
     * Get count of missing permissions
     */
    fun getMissingPermissionCount(context: Context): Int {
        return getPermissionStatus(context).count { !it.isGranted }
    }
    
    /**
     * Open notification listener settings
     */
    fun openNotificationListenerSettings(context: Context) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
    
    /**
     * Open battery optimization settings
     */
    fun openBatteryOptimizationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
    
    /**
     * Open app settings for manual permission granting
     */
    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
