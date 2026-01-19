package com.myparentalcontrol.shared.streaming.enums

/**
 * Defines the WebRTC connection states
 */
enum class ConnectionState {
    /** Initial state - not connected */
    DISCONNECTED,
    
    /** Connecting to peer */
    CONNECTING,
    
    /** Successfully connected and streaming */
    CONNECTED,
    
    /** Connection failed */
    FAILED,
    
    /** Connection was closed */
    CLOSED,
    
    /** Reconnecting after a disconnection */
    RECONNECTING;
    
    fun isActive(): Boolean = this == CONNECTED || this == CONNECTING || this == RECONNECTING
    
    companion object {
        fun fromString(value: String?): ConnectionState {
            return values().find { it.name.equals(value, ignoreCase = true) } ?: DISCONNECTED
        }
    }
}
