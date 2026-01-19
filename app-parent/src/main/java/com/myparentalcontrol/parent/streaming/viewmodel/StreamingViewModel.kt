package com.myparentalcontrol.parent.streaming.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myparentalcontrol.parent.streaming.core.SignalingManager
import com.myparentalcontrol.parent.streaming.core.WebRTCManager
import com.myparentalcontrol.shared.streaming.enums.AudioSource
import com.myparentalcontrol.shared.streaming.enums.ConnectionState
import com.myparentalcontrol.shared.streaming.enums.StreamType
import com.myparentalcontrol.shared.streaming.enums.VideoQuality
import com.myparentalcontrol.shared.streaming.models.StreamConfig
import com.myparentalcontrol.shared.streaming.models.StreamRequest
import com.myparentalcontrol.shared.streaming.models.StreamStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing streaming state in parent app
 */
@HiltViewModel
class StreamingViewModel @Inject constructor(
    private val webRTCManager: WebRTCManager,
    private val signalingManager: SignalingManager
) : ViewModel() {
    
    companion object {
        private const val TAG = "StreamingViewModel"
    }
    
    // UI State
    private val _uiState = MutableStateFlow(StreamingUiState())
    val uiState: StateFlow<StreamingUiState> = _uiState.asStateFlow()
    
    // Current child device being viewed
    private var currentChildDeviceId: String? = null
    private var parentId: String = ""
    
    init {
        // Observe WebRTC connection state
        viewModelScope.launch {
            webRTCManager.connectionState.collect { state ->
                _uiState.update { it.copy(connectionState = state) }
            }
        }
        
        // Observe video/audio availability
        viewModelScope.launch {
            webRTCManager.hasVideo.collect { hasVideo ->
                _uiState.update { it.copy(hasVideo = hasVideo) }
            }
        }
        
        viewModelScope.launch {
            webRTCManager.hasAudio.collect { hasAudio ->
                _uiState.update { it.copy(hasAudio = hasAudio) }
            }
        }
    }
    
    /**
     * Initialize the streaming system
     */
    fun initialize(parentId: String) {
        Log.d(TAG, "Initializing streaming for parent: $parentId")
        this.parentId = parentId
        
        webRTCManager.initialize()
        signalingManager.initialize(parentId)
        
        setupWebRTCCallbacks()
    }
    
    /**
     * Set up WebRTC callbacks
     */
    private fun setupWebRTCCallbacks() {
        webRTCManager.onAnswerCreated = { answer ->
            viewModelScope.launch {
                currentChildDeviceId?.let { deviceId ->
                    try {
                        signalingManager.sendAnswer(deviceId, answer)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to send answer", e)
                        _uiState.update { it.copy(error = "Failed to connect: ${e.message}") }
                    }
                }
            }
        }
        
        webRTCManager.onIceCandidate = { candidate ->
            viewModelScope.launch {
                currentChildDeviceId?.let { deviceId ->
                    try {
                        signalingManager.sendIceCandidate(deviceId, candidate)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to send ICE candidate", e)
                    }
                }
            }
        }
        
        webRTCManager.onConnectionStateChanged = { state ->
            Log.d(TAG, "Connection state changed: $state")
            if (state == ConnectionState.FAILED) {
                _uiState.update { it.copy(error = "Connection failed") }
            }
        }
    }
    
    /**
     * Request camera stream from child device
     */
    fun requestCameraStream(
        childDeviceId: String,
        cameraType: StreamType = StreamType.CAMERA_FRONT,
        withAudio: Boolean = false,
        audioSource: AudioSource = AudioSource.MICROPHONE,
        videoQuality: VideoQuality = VideoQuality.MEDIUM
    ) {
        Log.d(TAG, "Requesting camera stream from $childDeviceId")
        
        val request = StreamRequest.cameraRequest(
            cameraType = cameraType,
            parentId = parentId,
            withAudio = withAudio,
            audioSource = audioSource,
            videoQuality = videoQuality
        )
        
        startStreamRequest(childDeviceId, request)
    }
    
    /**
     * Request screen stream from child device
     */
    fun requestScreenStream(
        childDeviceId: String,
        withAudio: Boolean = false,
        audioSource: AudioSource = AudioSource.DEVICE_AUDIO,
        videoQuality: VideoQuality = VideoQuality.MEDIUM
    ) {
        Log.d(TAG, "Requesting screen stream from $childDeviceId")
        
        val request = StreamRequest.screenRequest(
            parentId = parentId,
            withAudio = withAudio,
            audioSource = audioSource,
            videoQuality = videoQuality
        )
        
        startStreamRequest(childDeviceId, request)
    }
    
    /**
     * Request audio-only stream from child device
     */
    fun requestAudioStream(
        childDeviceId: String,
        audioSource: AudioSource = AudioSource.MICROPHONE
    ) {
        Log.d(TAG, "Requesting audio stream from $childDeviceId")
        
        val request = StreamRequest.audioOnlyRequest(
            parentId = parentId,
            audioSource = audioSource
        )
        
        startStreamRequest(childDeviceId, request)
    }
    
    /**
     * Start stream request
     */
    private fun startStreamRequest(childDeviceId: String, request: StreamRequest) {
        currentChildDeviceId = childDeviceId
        
        _uiState.update { 
            it.copy(
                isLoading = true,
                error = null,
                currentStreamType = request.type,
                audioEnabled = request.audioEnabled,
                audioSource = request.audioSource
            )
        }
        
        viewModelScope.launch {
            try {
                // Send stream request
                signalingManager.sendStreamRequest(childDeviceId, request)
                
                // Start WebRTC
                val config = StreamConfig.fromRequest(request)
                webRTCManager.startReceiving(config, parentId)
                
                // Listen for offer from child
                listenForOffer(childDeviceId)
                
                // Listen for ICE candidates
                listenForIceCandidates(childDeviceId)
                
                // Listen for stream status
                listenForStreamStatus(childDeviceId)
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start stream request", e)
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "Failed to request stream: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * Listen for offer from child
     */
    private fun listenForOffer(childDeviceId: String) {
        viewModelScope.launch {
            signalingManager.observeOffer(childDeviceId).collect { offer ->
                if (offer != null) {
                    Log.d(TAG, "Received offer from child")
                    webRTCManager.setRemoteOffer(offer)
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }
    
    /**
     * Listen for ICE candidates from child
     */
    private fun listenForIceCandidates(childDeviceId: String) {
        viewModelScope.launch {
            signalingManager.observeChildIceCandidates(childDeviceId).collect { candidate ->
                Log.d(TAG, "Received ICE candidate from child")
                webRTCManager.addIceCandidate(candidate)
            }
        }
    }
    
    /**
     * Listen for stream status from child
     */
    private fun listenForStreamStatus(childDeviceId: String) {
        viewModelScope.launch {
            signalingManager.observeStreamStatus(childDeviceId).collect { status ->
                Log.d(TAG, "Stream status: $status")
                _uiState.update { it.copy(streamStatus = status) }
            }
        }
    }
    
    /**
     * Stop current stream
     */
    fun stopStream() {
        Log.d(TAG, "Stopping stream")
        
        viewModelScope.launch {
            currentChildDeviceId?.let { deviceId ->
                try {
                    signalingManager.cancelStreamRequest(deviceId)
                    signalingManager.clearSignalingData(deviceId)
                    signalingManager.stopListening(deviceId)
                } catch (e: Exception) {
                    Log.e(TAG, "Error stopping stream", e)
                }
            }
            
            webRTCManager.stopReceiving()
            
            _uiState.update { 
                StreamingUiState() // Reset to initial state
            }
            
            currentChildDeviceId = null
        }
    }
    
    /**
     * Toggle audio
     */
    fun toggleAudio() {
        val currentState = _uiState.value.isAudioMuted
        webRTCManager.setAudioEnabled(currentState) // If muted, enable; if not muted, disable
        _uiState.update { it.copy(isAudioMuted = !currentState) }
    }
    
    /**
     * Set audio muted state
     */
    fun setAudioMuted(muted: Boolean) {
        webRTCManager.setAudioEnabled(!muted)
        _uiState.update { it.copy(isAudioMuted = muted) }
    }
    
    /**
     * Toggle video
     */
    fun toggleVideo() {
        val currentState = _uiState.value.isVideoPaused
        webRTCManager.setVideoEnabled(currentState) // If paused, enable; if not paused, disable
        _uiState.update { it.copy(isVideoPaused = !currentState) }
    }
    
    /**
     * Clear error
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    /**
     * Get WebRTC manager for video renderer setup
     */
    fun getWebRTCManager(): WebRTCManager = webRTCManager
    
    override fun onCleared() {
        super.onCleared()
        stopStream()
        webRTCManager.release()
    }
}

/**
 * UI State for streaming screen
 */
data class StreamingUiState(
    val isLoading: Boolean = false,
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val streamStatus: StreamStatus = StreamStatus(),
    val currentStreamType: StreamType? = null,
    val audioEnabled: Boolean = false,
    val audioSource: AudioSource = AudioSource.NONE,
    val hasVideo: Boolean = false,
    val hasAudio: Boolean = false,
    val isAudioMuted: Boolean = false,
    val isVideoPaused: Boolean = false,
    val error: String? = null
) {
    val isConnected: Boolean get() = connectionState == ConnectionState.CONNECTED
    val isConnecting: Boolean get() = connectionState == ConnectionState.CONNECTING
    val isStreaming: Boolean get() = streamStatus.isStreaming
}
