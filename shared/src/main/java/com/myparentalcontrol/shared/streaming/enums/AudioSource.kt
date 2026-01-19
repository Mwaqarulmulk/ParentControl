package com.myparentalcontrol.shared.streaming.enums

/**
 * Defines the audio source for streaming
 */
enum class AudioSource {
    /** Microphone audio - captures surrounding sounds */
    MICROPHONE,
    
    /** Device audio - captures app sounds, music, etc. (requires Android 10+) */
    DEVICE_AUDIO,
    
    /** Both microphone and device audio mixed together */
    BOTH,
    
    /** No audio - video only stream */
    NONE;
    
    companion object {
        fun fromString(value: String?): AudioSource {
            return values().find { it.name.equals(value, ignoreCase = true) } ?: NONE
        }
    }
}
