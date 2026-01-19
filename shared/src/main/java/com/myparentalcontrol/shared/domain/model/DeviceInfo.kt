package com.myparentalcontrol.shared.domain.model

data class DeviceInfo(
    val deviceId: String = "",
    val deviceName: String = "",
    val deviceModel: String = "",
    val androidVersion: String = "",
    val appVersion: String = "",
    val deviceType: String = "", // "child" or "parent"
    val parentId: String = "", // User ID of the parent
    val createdAt: Long = System.currentTimeMillis(),
    val lastUpdated: Long = System.currentTimeMillis()
)

data class DeviceStatus(
    val deviceId: String = "",
    val isOnline: Boolean = false,
    val lastSeen: Long = System.currentTimeMillis(),
    val batteryLevel: Int = 0,
    val isCharging: Boolean = false,
    val networkType: String = "", // "wifi", "mobile", "none"
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val locationUpdatedAt: Long = 0
)
