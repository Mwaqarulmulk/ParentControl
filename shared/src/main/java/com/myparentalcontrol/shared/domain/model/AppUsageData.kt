package com.myparentalcontrol.shared.domain.model

data class AppUsageData(
    val id: String = "",
    val deviceId: String = "",
    val packageName: String = "",
    val appName: String = "",
    val usageTimeMillis: Long = 0,
    val lastUsed: Long = 0,
    val date: String = "", // Format: "yyyy-MM-dd"
    val launchCount: Int = 0,
    val category: String = "" // App category if available
)

data class InstalledApp(
    val packageName: String = "",
    val appName: String = "",
    val versionName: String = "",
    val versionCode: Long = 0,
    val installedAt: Long = 0,
    val isSystemApp: Boolean = false,
    val category: String = "",
    val iconBase64: String = "" // Base64 encoded app icon (optional)
)

data class AppInstallEvent(
    val id: String = "",
    val deviceId: String = "",
    val packageName: String = "",
    val appName: String = "",
    val eventType: String = "", // "installed" or "uninstalled"
    val timestamp: Long = System.currentTimeMillis()
)
