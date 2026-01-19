package com.myparentalcontrol.shared.domain.model

data class RemoteCommand(
    val id: String = "",
    val deviceId: String = "",
    val commandType: String = "",
    val payload: Map<String, Any> = emptyMap(),
    val status: CommandStatus = CommandStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val executedAt: Long = 0,
    val response: String = "",
    val errorMessage: String = ""
)

enum class CommandStatus {
    PENDING,
    EXECUTING,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class StreamingSession(
    val sessionId: String = "",
    val deviceId: String = "",
    val parentDeviceId: String = "",
    val streamType: StreamType = StreamType.CAMERA,
    val status: StreamStatus = StreamStatus.IDLE,
    val startedAt: Long = 0,
    val endedAt: Long = 0,
    val cameraFacing: CameraFacing = CameraFacing.BACK
)

enum class StreamType {
    CAMERA,
    SCREEN,
    AUDIO
}

enum class StreamStatus {
    IDLE,
    CONNECTING,
    CONNECTED,
    STREAMING,
    DISCONNECTED,
    ERROR
}

enum class CameraFacing {
    FRONT,
    BACK
}

// WebRTC Signaling Models
data class SignalingMessage(
    val type: SignalingType = SignalingType.OFFER,
    val sessionId: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val sdp: String = "",
    val candidate: IceCandidate? = null,
    val timestamp: Long = System.currentTimeMillis()
)

enum class SignalingType {
    OFFER,
    ANSWER,
    ICE_CANDIDATE,
    HANGUP
}

data class IceCandidate(
    val sdpMid: String = "",
    val sdpMLineIndex: Int = 0,
    val candidate: String = ""
)
