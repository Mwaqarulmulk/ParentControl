package com.myparentalcontrol.shared.data.supabase

/**
 * Supabase configuration for the Parental Control App
 */
object SupabaseConfig {
    // Supabase project URL
    const val SUPABASE_URL = "https://nvtwvvnwytxwimlvtjjv.supabase.co"
    
    // Supabase anon/public key (safe to use in client apps)
    const val SUPABASE_ANON_KEY = "sb_publishable_0JZ__54BsfDHTicn9VgJMQ_q7iqx5mo"
    
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
