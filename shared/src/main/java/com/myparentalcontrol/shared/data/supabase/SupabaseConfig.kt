package com.myparentalcontrol.shared.data.supabase

import com.myparentalcontrol.shared.BuildConfig

/**
 * Supabase configuration for the Parental Control App
 * IMPORTANT: Add your actual keys to local.properties file
 */
object SupabaseConfig {
    // Supabase project URL - loaded from BuildConfig
    val SUPABASE_URL: String = BuildConfig.SUPABASE_URL
    
    // Supabase anon/public key - loaded from BuildConfig
    val SUPABASE_ANON_KEY: String = BuildConfig.SUPABASE_ANON_KEY
    
    // Database table names
    object Tables {
        const val USERS = "users"
        const val DEVICES = "devices"
        const val PAIRING_CODES = "pairing_codes"
        const val LOCATIONS = "locations"
        const val COMMANDS = "commands"
        const val NOTIFICATIONS = "notifications"
        const val APP_USAGE = "app_usage"
        const val BLOCKED_APPS = "blocked_apps"
        const val SCREEN_TIME_LIMITS = "screen_time_limits"
        const val GEOFENCES = "geofences"
        const val GEOFENCE_EVENTS = "geofence_events"
        const val SNAPSHOTS = "snapshots"
        const val CALL_LOGS = "call_logs"
        const val SMS_LOGS = "sms_logs"
        const val ALERTS = "alerts"
        const val SIGNALING = "signaling"
    }
    
    // Storage buckets
    object Buckets {
        const val SNAPSHOTS = "snapshots"
        const val RECORDINGS = "recordings"
        const val AVATARS = "avatars"
    }
    
    // Realtime channels
    object Channels {
        const val DEVICES = "devices"
        const val COMMANDS = "commands"
        const val LOCATIONS = "locations"
        const val ALERTS = "alerts"
        const val SIGNALING = "signaling"
    }
}
