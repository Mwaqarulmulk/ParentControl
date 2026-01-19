package com.myparentalcontrol.shared.streaming.constants

/**
 * Constants for Firebase Realtime Database signaling paths
 */
object SignalingConstants {
    
    // Root paths
    const val SIGNALING_ROOT = "signaling"
    
    // Child paths under /signaling/{deviceId}/
    const val STREAM_REQUEST = "streamRequest"
    const val OFFER = "offer"
    const val ANSWER = "answer"
    const val CHILD_ICE_CANDIDATES = "childIceCandidates"
    const val PARENT_ICE_CANDIDATES = "parentIceCandidates"
    const val STREAM_STATUS = "streamStatus"
    
    // Stream request fields
    const val FIELD_TYPE = "type"
    const val FIELD_AUDIO_ENABLED = "audioEnabled"
    const val FIELD_AUDIO_SOURCE = "audioSource"
    const val FIELD_REQUESTED_BY = "requestedBy"
    const val FIELD_TIMESTAMP = "timestamp"
    const val FIELD_IS_ACTIVE = "isActive"
    const val FIELD_VIDEO_QUALITY = "videoQuality"
    const val FIELD_AUDIO_QUALITY = "audioQuality"
    
    // Stream status fields
    const val FIELD_IS_STREAMING = "isStreaming"
    const val FIELD_STREAM_TYPE = "streamType"
    const val FIELD_CONNECTION_STATE = "connectionState"
    const val FIELD_STARTED_AT = "startedAt"
    const val FIELD_ERROR = "error"
    
    // SDP fields
    const val FIELD_SDP = "sdp"
    const val FIELD_SDP_TYPE = "type"
    const val FIELD_SENDER_ID = "senderId"
    
    // ICE candidate fields
    const val FIELD_SDP_MID = "sdpMid"
    const val FIELD_SDP_MLINE_INDEX = "sdpMLineIndex"
    const val FIELD_CANDIDATE = "candidate"
    
    // Timeouts (in milliseconds)
    const val OFFER_TIMEOUT_MS = 30_000L
    const val ANSWER_TIMEOUT_MS = 30_000L
    const val ICE_GATHERING_TIMEOUT_MS = 10_000L
    const val CONNECTION_TIMEOUT_MS = 60_000L
    const val RECONNECT_DELAY_MS = 5_000L
    const val MAX_RECONNECT_ATTEMPTS = 3
    
    // Quality thresholds
    const val MIN_BITRATE_BPS = 100_000
    const val MAX_BITRATE_BPS = 5_000_000
    const val PACKET_LOSS_THRESHOLD = 0.05f // 5%
    const val RTT_THRESHOLD_MS = 500
    
    /**
     * Get the full path for a device's signaling data
     */
    fun getDevicePath(deviceId: String): String {
        return "$SIGNALING_ROOT/$deviceId"
    }
    
    /**
     * Get the full path for stream request
     */
    fun getStreamRequestPath(deviceId: String): String {
        return "${getDevicePath(deviceId)}/$STREAM_REQUEST"
    }
    
    /**
     * Get the full path for offer
     */
    fun getOfferPath(deviceId: String): String {
        return "${getDevicePath(deviceId)}/$OFFER"
    }
    
    /**
     * Get the full path for answer
     */
    fun getAnswerPath(deviceId: String): String {
        return "${getDevicePath(deviceId)}/$ANSWER"
    }
    
    /**
     * Get the full path for child ICE candidates
     */
    fun getChildIceCandidatesPath(deviceId: String): String {
        return "${getDevicePath(deviceId)}/$CHILD_ICE_CANDIDATES"
    }
    
    /**
     * Get the full path for parent ICE candidates
     */
    fun getParentIceCandidatesPath(deviceId: String): String {
        return "${getDevicePath(deviceId)}/$PARENT_ICE_CANDIDATES"
    }
    
    /**
     * Get the full path for stream status
     */
    fun getStreamStatusPath(deviceId: String): String {
        return "${getDevicePath(deviceId)}/$STREAM_STATUS"
    }
}

/**
 * WebRTC codec constants
 */
object CodecConstants {
    // Video codecs
    const val CODEC_VP8 = "VP8"
    const val CODEC_VP9 = "VP9"
    const val CODEC_H264 = "H264"
    const val CODEC_AV1 = "AV1"
    
    // Audio codecs
    const val CODEC_OPUS = "opus"
    const val CODEC_ISAC = "ISAC"
    const val CODEC_G722 = "G722"
    
    // Default preferences
    val VIDEO_CODEC_PREFERENCE = listOf(CODEC_VP8, CODEC_H264, CODEC_VP9)
    val AUDIO_CODEC_PREFERENCE = listOf(CODEC_OPUS, CODEC_ISAC)
}

/**
 * Media constraints constants
 */
object MediaConstraints {
    // Video constraints
    const val VIDEO_WIDTH = "width"
    const val VIDEO_HEIGHT = "height"
    const val VIDEO_FRAME_RATE = "frameRate"
    const val VIDEO_FACING_MODE = "facingMode"
    const val FACING_MODE_USER = "user"
    const val FACING_MODE_ENVIRONMENT = "environment"
    
    // Audio constraints
    const val AUDIO_ECHO_CANCELLATION = "echoCancellation"
    const val AUDIO_NOISE_SUPPRESSION = "noiseSuppression"
    const val AUDIO_AUTO_GAIN_CONTROL = "autoGainControl"
    const val AUDIO_SAMPLE_RATE = "sampleRate"
    const val AUDIO_CHANNEL_COUNT = "channelCount"
    
    // SDP constraints
    const val OFFER_TO_RECEIVE_AUDIO = "OfferToReceiveAudio"
    const val OFFER_TO_RECEIVE_VIDEO = "OfferToReceiveVideo"
    const val ICE_RESTART = "IceRestart"
}
