package com.myparentalcontrol.shared

/**
 * Base constants and utilities for the Parental Control App
 */
object ParentalControlApp {
    const val TAG = "ParentalControl"
    
    // Firebase Collections
    object FirebaseCollections {
        const val USERS = "users"
        const val DEVICES = "devices"
        const val APP_USAGE = "appUsage"
        const val LOCATIONS = "locations"
        const val CALL_LOGS = "callLogs"
        const val SMS_LOGS = "smsLogs"
        const val BLOCKED_APPS = "blockedApps"
        const val SCREEN_TIME_RULES = "screenTimeRules"
        const val COMMANDS = "commands"
    }
    
    // Realtime Database Paths
    object RealtimePaths {
        const val PRESENCE = "presence"
        const val SIGNALING = "signaling"
        const val COMMANDS = "commands"
        const val LOCATIONS = "locations"
        const val DEVICES = "devices"
        const val NOTIFICATIONS = "notifications"
    }
    
    // Notification Channels
    object NotificationChannels {
        const val MONITORING = "monitoring_channel"
        const val ALERTS = "alerts_channel"
        const val STREAMING = "streaming_channel"
    }
    
    // Command Types
    object CommandTypes {
        const val START_CAMERA_STREAM = "start_camera_stream"
        const val STOP_CAMERA_STREAM = "stop_camera_stream"
        const val START_SCREEN_MIRROR = "start_screen_mirror"
        const val STOP_SCREEN_MIRROR = "stop_screen_mirror"
        const val START_AUDIO_STREAM = "start_audio_stream"
        const val STOP_AUDIO_STREAM = "stop_audio_stream"
        const val UPDATE_LOCATION = "update_location"
        const val SYNC_DATA = "sync_data"
        const val LOCK_DEVICE = "lock_device"
        const val PLAY_SOUND = "play_sound"
    }
    
    // Device Types
    object DeviceTypes {
        const val CHILD = "child"
        const val PARENT = "parent"
    }
}
