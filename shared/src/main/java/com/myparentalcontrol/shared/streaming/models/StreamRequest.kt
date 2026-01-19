package com.myparentalcontrol.shared.streaming.models

import com.myparentalcontrol.shared.streaming.enums.AudioSource
import com.myparentalcontrol.shared.streaming.enums.StreamType
import com.myparentalcontrol.shared.streaming.enums.VideoQuality

/**
 * Represents a stream request from parent to child device
 * This is stored in Firebase Realtime Database under /signaling/{childDeviceId}/streamRequest
 */
data class StreamRequest(
    /** Type of stream requested */
    val type: StreamType = StreamType.CAMERA_FRONT,
    
    /** Whether audio is enabled for this stream */
    val audioEnabled: Boolean = false,
    
    /** Source of audio if enabled */
    val audioSource: AudioSource = AudioSource.NONE,
    
    /** Parent user ID who requested the stream */
    val requestedBy: String = "",
    
    /** Timestamp when the request was made */
    val timestamp: Long = System.currentTimeMillis(),
    
    /** Whether the stream request is currently active */
    val isActive: Boolean = true,
    
    /** Video quality setting */
    val videoQuality: VideoQuality = VideoQuality.MEDIUM,
    
    /** Audio quality setting (sample rate in Hz) */
    val audioQuality: AudioQuality = AudioQuality.MEDIUM
) {
    /** Convert to Firebase-compatible map */
    fun toMap(): Map<String, Any?> = mapOf(
        "type" to type.name,
        "audioEnabled" to audioEnabled,
        "audioSource" to audioSource.name,
        "requestedBy" to requestedBy,
        "timestamp" to timestamp,
        "isActive" to isActive,
        "videoQuality" to videoQuality.name,
        "audioQuality" to audioQuality.name
    )
    
    companion object {
        /** Create StreamRequest from Firebase data map */
        fun fromMap(map: Map<String, Any?>): StreamRequest {
            return StreamRequest(
                type = StreamType.fromString(map["type"] as? String) ?: StreamType.CAMERA_FRONT,
                audioEnabled = map["audioEnabled"] as? Boolean ?: false,
                audioSource = AudioSource.fromString(map["audioSource"] as? String),
                requestedBy = map["requestedBy"] as? String ?: "",
                timestamp = (map["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                isActive = map["isActive"] as? Boolean ?: true,
                videoQuality = VideoQuality.fromString(map["videoQuality"] as? String),
                audioQuality = AudioQuality.fromString(map["audioQuality"] as? String)
            )
        }
        
        /** Create a camera stream request */
        fun cameraRequest(
            cameraType: StreamType = StreamType.CAMERA_FRONT,
            parentId: String,
            withAudio: Boolean = false,
            audioSource: AudioSource = AudioSource.MICROPHONE,
            videoQuality: VideoQuality = VideoQuality.MEDIUM
        ): StreamRequest {
            return StreamRequest(
                type = cameraType,
                audioEnabled = withAudio,
                audioSource = if (withAudio) audioSource else AudioSource.NONE,
                requestedBy = parentId,
                videoQuality = videoQuality
            )
        }
        
        /** Create a screen stream request */
        fun screenRequest(
            parentId: String,
            withAudio: Boolean = false,
            audioSource: AudioSource = AudioSource.DEVICE_AUDIO,
            videoQuality: VideoQuality = VideoQuality.MEDIUM
        ): StreamRequest {
            return StreamRequest(
                type = StreamType.SCREEN,
                audioEnabled = withAudio,
                audioSource = if (withAudio) audioSource else AudioSource.NONE,
                requestedBy = parentId,
                videoQuality = videoQuality
            )
        }
        
        /** Create an audio-only stream request */
        fun audioOnlyRequest(
            parentId: String,
            audioSource: AudioSource = AudioSource.MICROPHONE
        ): StreamRequest {
            return StreamRequest(
                type = StreamType.AUDIO_ONLY,
                audioEnabled = true,
                audioSource = audioSource,
                requestedBy = parentId
            )
        }
    }
}

/**
 * Audio quality settings
 */
enum class AudioQuality(
    val sampleRate: Int,
    val bitrate: Int,
    val channels: Int
) {
    HIGH(48000, 128000, 2),
    MEDIUM(44100, 96000, 1),
    LOW(22050, 64000, 1);
    
    companion object {
        fun fromString(value: String?): AudioQuality {
            return values().find { it.name.equals(value, ignoreCase = true) } ?: MEDIUM
        }
    }
}
