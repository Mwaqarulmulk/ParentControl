package com.myparentalcontrol.shared.domain.model

data class AppBlockRule(
    val id: String = "",
    val deviceId: String = "",
    val packageName: String = "",
    val appName: String = "",
    val isBlocked: Boolean = false,
    val blockSchedule: BlockSchedule? = null, // Optional schedule-based blocking
    val dailyTimeLimitMinutes: Int = 0, // 0 means no limit
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class BlockSchedule(
    val isEnabled: Boolean = false,
    val startTimeMinutes: Int = 0, // Minutes from midnight (e.g., 1320 = 22:00)
    val endTimeMinutes: Int = 0, // Minutes from midnight (e.g., 420 = 07:00)
    val daysOfWeek: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7) // 1 = Monday, 7 = Sunday
)

data class ScreenTimeRule(
    val id: String = "",
    val deviceId: String = "",
    val dailyLimitMinutes: Int = 120, // Default 2 hours
    val isEnabled: Boolean = true,
    val bedtimeEnabled: Boolean = false,
    val bedtimeStart: Int = 1320, // 22:00 in minutes from midnight
    val bedtimeEnd: Int = 420, // 07:00 in minutes from midnight
    val bedtimeDays: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7),
    val allowedAppsInBedtime: List<String> = emptyList(), // Package names
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class ScreenTimeStatus(
    val deviceId: String = "",
    val date: String = "", // Format: "yyyy-MM-dd"
    val usedMinutes: Int = 0,
    val limitMinutes: Int = 0,
    val remainingMinutes: Int = 0,
    val isLimitReached: Boolean = false,
    val isBedtime: Boolean = false
)
