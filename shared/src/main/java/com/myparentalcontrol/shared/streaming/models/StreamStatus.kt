package com.myparentalcontrol.shared.streaming.models

import com.myparentalcontrol.shared.streaming.enums.AudioSource
import com.myparentalcontrol.shared.streaming.enums.ConnectionState
import com.myparentalcontrol.shared.streaming.enums.StreamType

/**
 * Represents the current status of a stream
 * This is stored in Firebase Realtime Database under /signaling/{childDeviceId}/streamStatus
 */
data class StreamStatus(
    /** Whether streaming is currently active */
    val isStreaming: Boolean = false,
    
    /** Current stream type */
    val streamType: StreamType? = null,
    
    /** Whether audio is enabled */
    val audioEnabled: Boolean = false,
    
    /** Current audio source */
    val audioSource: AudioSource = AudioSource.NONE,
    
    /** Connection state */
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    
    /** Timestamp when streaming started */
    val startedAt: Long? = null,
    
    /** Error message if any */
    val error: String? = null,
    
    /** Current video bitrate (for monitoring) */
    val videoBitrate: Int? = null,
    
    /** Current audio bitrate (for monitoring) */
    val audioBitrate: Int? = null,
    
    /** Frames per second (for monitoring) */
    val fps: Int? = null,
    
    /** Round trip time in milliseconds (for monitoring) */
    val rtt: Int? = null,
    
    /** Packet loss percentage (for monitoring) */
    val packetLoss: Float? = null
) {
    /** Convert to Firebase-compatible map */
    fun toMap(): Map<String, Any?> = mapOf(
        "isStreaming" to isStreaming,
        "streamType" to streamType?.name,
        "audioEnabled" to audioEnabled,
        "audioSource" to audioSource.name,
        "connectionState" to connectionState.name,
        "startedAt" to startedAt,
        "error" to error,
        "videoBitrate" to videoBitrate,
        "audioBitrate" to audioBitrate,
        "fps" to fps,
        "rtt" to rtt,
        "packetLoss" to packetLoss
    )
    
    /** Get streaming duration in seconds */
    fun getDurationSeconds(): Long {
        return if (isStreaming && startedAt != null) {
            (System.currentTimeMillis() - startedAt) / 1000
        } else {
            0
        }
    }
    
    /** Get formatted duration string (HH:MM:SS) */
    fun getFormattedDuration(): String {
        val totalSeconds = getDurationSeconds()
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }
    
    companion object {
        /** Create StreamStatus from Firebase data map */
        fun fromMap(map: Map<String, Any?>): StreamStatus {
            return StreamStatus(
                isStreaming = map["isStreaming"] as? Boolean ?: false,
                streamType = StreamType.fromString(map["streamType"] as? String),
                audioEnabled = map["audioEnabled"] as? Boolean ?: false,
                audioSource = AudioSource.fromString(map["audioSource"] as? String),
                connectionState = ConnectionState.fromString(map["connectionState"] as? String),
                startedAt = (map["startedAt"] as? Number)?.toLong(),
                error = map["error"] as? String,
                videoBitrate = (map["videoBitrate"] as? Number)?.toInt(),
                audioBitrate = (map["audioBitrate"] as? Number)?.toInt(),
                fps = (map["fps"] as? Number)?.toInt(),
                rtt = (map["rtt"] as? Number)?.toInt(),
                packetLoss = (map["packetLoss"] as? Number)?.toFloat()
            )
        }
        
        /** Create initial streaming status */
        fun streaming(
            streamType: StreamType,
            audioEnabled: Boolean = false,
            audioSource: AudioSource = AudioSource.NONE
        ): StreamStatus {
            return StreamStatus(
                isStreaming = true,
                streamType = streamType,
                audioEnabled = audioEnabled,
                audioSource = audioSource,
                connectionState = ConnectionState.CONNECTED,
                startedAt = System.currentTimeMillis()
            )
        }
        
        /** Create connecting status */
        fun connecting(streamType: StreamType): StreamStatus {
            return StreamStatus(
                isStreaming = false,
                streamType = streamType,
                connectionState = ConnectionState.CONNECTING
            )
        }
        
        /** Create error status */
        fun error(errorMessage: String): StreamStatus {
            return StreamStatus(
                isStreaming = false,
                connectionState = ConnectionState.FAILED,
                error = errorMessage
            )
        }
        
        /** Create disconnected status */
        fun disconnected(): StreamStatus {
            return StreamStatus(
                isStreaming = false,
                connectionState = ConnectionState.DISCONNECTED
            )
        }
    }
}
