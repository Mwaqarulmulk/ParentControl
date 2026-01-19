package com.myparentalcontrol.shared.streaming.models

import com.myparentalcontrol.shared.streaming.enums.AudioSource
import com.myparentalcontrol.shared.streaming.enums.StreamType
import com.myparentalcontrol.shared.streaming.enums.VideoQuality

/**
 * Configuration for a streaming session
 * Used internally by the streaming services
 */
data class StreamConfig(
    /** Type of stream */
    val streamType: StreamType,
    
    /** Video configuration */
    val videoConfig: VideoConfig,
    
    /** Audio configuration */
    val audioConfig: AudioConfig,
    
    /** ICE servers for WebRTC */
    val iceServers: List<IceServer> = defaultIceServers()
) {
    companion object {
        /** Create config from stream request */
        fun fromRequest(request: StreamRequest): StreamConfig {
            return StreamConfig(
                streamType = request.type,
                videoConfig = VideoConfig.fromQuality(request.videoQuality),
                audioConfig = AudioConfig(
                    enabled = request.audioEnabled,
                    source = request.audioSource,
                    quality = request.audioQuality
                )
            )
        }
        
        /** Default ICE servers (STUN/TURN) */
        fun defaultIceServers(): List<IceServer> = listOf(
            IceServer(
                urls = listOf("stun:stun.l.google.com:19302"),
                username = null,
                credential = null
            ),
            IceServer(
                urls = listOf("stun:stun1.l.google.com:19302"),
                username = null,
                credential = null
            ),
            IceServer(
                urls = listOf("stun:stun2.l.google.com:19302"),
                username = null,
                credential = null
            )
            // Add TURN servers here for production
            // IceServer(
            //     urls = listOf("turn:your-turn-server.com:3478"),
            //     username = "username",
            //     credential = "password"
            // )
        )
    }
}

/**
 * Video configuration settings
 */
data class VideoConfig(
    /** Video width in pixels */
    val width: Int,
    
    /** Video height in pixels */
    val height: Int,
    
    /** Frame rate */
    val frameRate: Int,
    
    /** Target bitrate in bps */
    val bitrate: Int,
    
    /** Whether to use hardware encoding */
    val useHardwareEncoder: Boolean = true,
    
    /** Codec preference (VP8, VP9, H264) */
    val codecPreference: String = "VP8"
) {
    companion object {
        fun fromQuality(quality: VideoQuality): VideoConfig {
            return VideoConfig(
                width = quality.width,
                height = quality.height,
                frameRate = quality.frameRate,
                bitrate = quality.bitrate
            )
        }
    }
}

/**
 * Audio configuration settings
 */
data class AudioConfig(
    /** Whether audio is enabled */
    val enabled: Boolean = false,
    
    /** Audio source */
    val source: AudioSource = AudioSource.NONE,
    
    /** Audio quality settings */
    val quality: AudioQuality = AudioQuality.MEDIUM,
    
    /** Whether to use echo cancellation */
    val echoCancellation: Boolean = true,
    
    /** Whether to use noise suppression */
    val noiseSuppression: Boolean = true,
    
    /** Whether to use auto gain control */
    val autoGainControl: Boolean = true
) {
    /** Check if microphone is needed */
    fun needsMicrophone(): Boolean = enabled && 
        (source == AudioSource.MICROPHONE || source == AudioSource.BOTH)
    
    /** Check if device audio capture is needed */
    fun needsDeviceAudio(): Boolean = enabled && 
        (source == AudioSource.DEVICE_AUDIO || source == AudioSource.BOTH)
}

/**
 * ICE Server configuration for WebRTC
 */
data class IceServer(
    /** Server URLs (stun: or turn:) */
    val urls: List<String>,
    
    /** Username for TURN server authentication */
    val username: String?,
    
    /** Credential for TURN server authentication */
    val credential: String?
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "urls" to urls,
        "username" to username,
        "credential" to credential
    )
    
    companion object {
        fun fromMap(map: Map<String, Any?>): IceServer {
            @Suppress("UNCHECKED_CAST")
            return IceServer(
                urls = (map["urls"] as? List<String>) ?: emptyList(),
                username = map["username"] as? String,
                credential = map["credential"] as? String
            )
        }
    }
}
