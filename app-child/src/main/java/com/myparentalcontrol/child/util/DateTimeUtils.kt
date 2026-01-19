package com.myparentalcontrol.child.util

import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object DateTimeUtils {
    
    private const val DATE_FORMAT = "yyyy-MM-dd"
    private const val TIME_FORMAT = "HH:mm:ss"
    private const val DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss"
    private const val DISPLAY_FORMAT = "MMM dd, yyyy hh:mm a"
    
    /**
     * Get current date in yyyy-MM-dd format
     */
    fun getCurrentDate(): String {
        return SimpleDateFormat(DATE_FORMAT, Locale.getDefault()).format(Date())
    }
    
    /**
     * Get current time in HH:mm:ss format
     */
    fun getCurrentTime(): String {
        return SimpleDateFormat(TIME_FORMAT, Locale.getDefault()).format(Date())
    }
    
    /**
     * Get current timestamp in milliseconds
     */
    fun getCurrentTimestamp(): Long {
        return System.currentTimeMillis()
    }
    
    /**
     * Format timestamp to date string
     */
    fun formatTimestampToDate(timestamp: Long): String {
        return SimpleDateFormat(DATE_FORMAT, Locale.getDefault()).format(Date(timestamp))
    }
    
    /**
     * Format timestamp to datetime string
     */
    fun formatTimestampToDateTime(timestamp: Long): String {
        return SimpleDateFormat(DATETIME_FORMAT, Locale.getDefault()).format(Date(timestamp))
    }
    
    /**
     * Format timestamp to display format
     */
    fun formatTimestampToDisplay(timestamp: Long): String {
        return SimpleDateFormat(DISPLAY_FORMAT, Locale.getDefault()).format(Date(timestamp))
    }
    
    /**
     * Get start of day timestamp
     */
    fun getStartOfDayTimestamp(date: Date = Date()): Long {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
    
    /**
     * Get end of day timestamp
     */
    fun getEndOfDayTimestamp(date: Date = Date()): Long {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }
    
    /**
     * Convert milliseconds to human readable duration
     */
    fun formatDuration(millis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
        
        return when {
            hours > 0 -> String.format("%dh %dm", hours, minutes)
            minutes > 0 -> String.format("%dm %ds", minutes, seconds)
            else -> String.format("%ds", seconds)
        }
    }
    
    /**
     * Check if timestamp is today
     */
    fun isToday(timestamp: Long): Boolean {
        val today = getCurrentDate()
        val date = formatTimestampToDate(timestamp)
        return today == date
    }
    
    /**
     * Check if timestamp is within last N hours
     */
    fun isWithinLastHours(timestamp: Long, hours: Int): Boolean {
        val cutoff = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(hours.toLong())
        return timestamp >= cutoff
    }
    
    /**
     * Parse time string (HH:mm) to minutes since midnight
     */
    fun parseTimeToMinutes(timeString: String): Int {
        val parts = timeString.split(":")
        return if (parts.size == 2) {
            val hours = parts[0].toIntOrNull() ?: 0
            val minutes = parts[1].toIntOrNull() ?: 0
            hours * 60 + minutes
        } else {
            0
        }
    }
    
    /**
     * Get current minutes since midnight
     */
    fun getCurrentMinutesSinceMidnight(): Int {
        val calendar = Calendar.getInstance()
        return calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
    }
    
    /**
     * Check if current time is within bedtime schedule
     */
    fun isWithinBedtime(startTime: String, endTime: String): Boolean {
        val currentMinutes = getCurrentMinutesSinceMidnight()
        val startMinutes = parseTimeToMinutes(startTime)
        val endMinutes = parseTimeToMinutes(endTime)
        
        return if (startMinutes < endMinutes) {
            // Normal range (e.g., 22:00 - 23:00)
            currentMinutes in startMinutes until endMinutes
        } else {
            // Overnight range (e.g., 22:00 - 07:00)
            currentMinutes >= startMinutes || currentMinutes < endMinutes
        }
    }
}
