package com.myparentalcontrol.parent.data.model

/**
 * Child Device Model
 * Represents a paired child device with all its data
 */
data class ChildDevice(
    val deviceId: String = "",
    val deviceName: String = "",
    val deviceModel: String = "",
    val parentId: String = "",
    val pairedAt: Long = System.currentTimeMillis(),
    
    // Status
    val isOnline: Boolean = false,
    val lastSeen: Long = 0,
    val batteryLevel: Int = 0,
    val isCharging: Boolean = false,
    val networkType: String = "",
    
    // Location
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val locationUpdatedAt: Long = 0,
    
    // Features
    val notificationAccessEnabled: Boolean = false,
    
    // Settings
    val nickname: String = ""
) {
    fun getDisplayName(): String = nickname.ifEmpty { deviceName }
    
    fun getStatusText(): String {
        return if (isOnline) {
            "Online"
        } else if (lastSeen > 0) {
            "Last seen: ${getTimeAgo(lastSeen)}"
        } else {
            "Never connected"
        }
    }
    
    private fun getTimeAgo(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        val minutes = diff / 60000
        val hours = minutes / 60
        val days = hours / 24
        
        return when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "$minutes min ago"
            hours < 24 -> "$hours hr ago"
            else -> "$days days ago"
        }
    }
}

data class LocationHistory(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timestamp: Long = 0,
    val accuracy: Float = 0f
)

data class NotificationData(
    val id: String = "",
    val appName: String = "",
    val packageName: String = "",
    val title: String = "",
    val text: String = "",
    val timestamp: Long = 0,
    val isOngoing: Boolean = false
)
