package com.myparentalcontrol.shared.streaming.models

/**
 * WebRTC Session Description Protocol (SDP) data
 * Used for offer/answer exchange in WebRTC signaling
 */
data class SignalingData(
    /** SDP type: "offer" or "answer" */
    val type: String,
    
    /** SDP description string */
    val sdp: String,
    
    /** Sender device ID */
    val senderId: String,
    
    /** Timestamp when created */
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "type" to type,
        "sdp" to sdp,
        "senderId" to senderId,
        "timestamp" to timestamp
    )
    
    companion object {
        fun fromMap(map: Map<String, Any?>): SignalingData? {
            val type = map["type"] as? String ?: return null
            val sdp = map["sdp"] as? String ?: return null
            val senderId = map["senderId"] as? String ?: return null
            
            return SignalingData(
                type = type,
                sdp = sdp,
                senderId = senderId,
                timestamp = (map["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }
        
        /** Create an offer */
        fun offer(sdp: String, senderId: String): SignalingData {
            return SignalingData(
                type = "offer",
                sdp = sdp,
                senderId = senderId
            )
        }
        
        /** Create an answer */
        fun answer(sdp: String, senderId: String): SignalingData {
            return SignalingData(
                type = "answer",
                sdp = sdp,
                senderId = senderId
            )
        }
    }
}

/**
 * ICE Candidate data for WebRTC connectivity
 */
data class IceCandidateData(
    /** SDP mid (media stream identification) */
    val sdpMid: String?,
    
    /** SDP m-line index */
    val sdpMLineIndex: Int,
    
    /** ICE candidate string */
    val candidate: String,
    
    /** Sender device ID */
    val senderId: String,
    
    /** Timestamp when created */
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "sdpMid" to sdpMid,
        "sdpMLineIndex" to sdpMLineIndex,
        "candidate" to candidate,
        "senderId" to senderId,
        "timestamp" to timestamp
    )
    
    companion object {
        fun fromMap(map: Map<String, Any?>): IceCandidateData? {
            val candidate = map["candidate"] as? String ?: return null
            val senderId = map["senderId"] as? String ?: return null
            
            return IceCandidateData(
                sdpMid = map["sdpMid"] as? String,
                sdpMLineIndex = (map["sdpMLineIndex"] as? Number)?.toInt() ?: 0,
                candidate = candidate,
                senderId = senderId,
                timestamp = (map["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }
    }
}

/**
 * Complete signaling session data
 * Represents all signaling data for a streaming session
 */
data class SignalingSession(
    /** Child device ID */
    val childDeviceId: String,
    
    /** Stream request from parent */
    val streamRequest: StreamRequest? = null,
    
    /** SDP offer from child */
    val offer: SignalingData? = null,
    
    /** SDP answer from parent */
    val answer: SignalingData? = null,
    
    /** ICE candidates from child */
    val childIceCandidates: List<IceCandidateData> = emptyList(),
    
    /** ICE candidates from parent */
    val parentIceCandidates: List<IceCandidateData> = emptyList(),
    
    /** Current stream status */
    val streamStatus: StreamStatus = StreamStatus()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "streamRequest" to streamRequest?.toMap(),
        "offer" to offer?.toMap(),
        "answer" to answer?.toMap(),
        "childIceCandidates" to childIceCandidates.associate { 
            it.timestamp.toString() to it.toMap() 
        },
        "parentIceCandidates" to parentIceCandidates.associate { 
            it.timestamp.toString() to it.toMap() 
        },
        "streamStatus" to streamStatus.toMap()
    )
}
