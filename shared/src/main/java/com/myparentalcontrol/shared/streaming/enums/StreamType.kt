package com.myparentalcontrol.shared.streaming.enums

/**
 * Defines the type of stream being requested or active
 */
enum class StreamType {
    /** Front-facing camera stream */
    CAMERA_FRONT,
    
    /** Back-facing camera stream */
    CAMERA_BACK,
    
    /** Screen capture/mirroring stream */
    SCREEN,
    
    /** Audio only stream (no video) */
    AUDIO_ONLY;
    
    companion object {
        fun fromString(value: String?): StreamType? {
            return values().find { it.name.equals(value, ignoreCase = true) }
        }
    }
}
