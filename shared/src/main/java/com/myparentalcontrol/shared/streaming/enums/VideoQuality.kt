package com.myparentalcontrol.shared.streaming.enums

/**
 * Defines the video quality settings for streaming
 */
enum class VideoQuality(
    val width: Int,
    val height: Int,
    val frameRate: Int,
    val bitrate: Int
) {
    /** High quality - 1080p at 30fps */
    HIGH(1920, 1080, 30, 2_500_000),
    
    /** Medium quality - 720p at 30fps */
    MEDIUM(1280, 720, 30, 1_500_000),
    
    /** Low quality - 480p at 24fps (for slow connections) */
    LOW(854, 480, 24, 800_000),
    
    /** Very low quality - 360p at 15fps (for very slow connections) */
    VERY_LOW(640, 360, 15, 400_000);
    
    companion object {
        fun fromString(value: String?): VideoQuality {
            return values().find { it.name.equals(value, ignoreCase = true) } ?: MEDIUM
        }
    }
}
