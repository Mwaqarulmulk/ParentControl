package com.myparentalcontrol.child.streaming.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.myparentalcontrol.child.R
import com.myparentalcontrol.child.streaming.audio.MicrophoneManager
import com.myparentalcontrol.child.streaming.core.SignalingManager
import com.myparentalcontrol.child.streaming.core.WebRTCManager
import com.myparentalcontrol.child.streaming.video.CameraManager
import com.myparentalcontrol.child.streaming.video.ScreenCaptureManager
import com.myparentalcontrol.shared.streaming.enums.AudioSource
import com.myparentalcontrol.shared.streaming.enums.StreamType
import com.myparentalcontrol.shared.streaming.models.StreamConfig
import com.myparentalcontrol.shared.streaming.models.StreamRequest
import com.myparentalcontrol.shared.streaming.models.StreamStatus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

/**
 * Foreground service for streaming video/audio to parent device
 * Handles camera, screen capture, and audio streaming via WebRTC
 */
@AndroidEntryPoint
class StreamingService : Service() {
    
    companion object {
        private const val TAG = "StreamingService"
        private const val NOTIFICATION_ID = 2001
        private const val CHANNEL_ID = "streaming_channel"
        
        const val ACTION_START_STREAMING = "com.myparentalcontrol.child.START_STREAMING"
        const val ACTION_STOP_STREAMING = "com.myparentalcontrol.child.STOP_STREAMING"
        const val EXTRA_DEVICE_ID = "device_id"
        
        fun startService(context: Context, deviceId: String) {
            val intent = Intent(context, StreamingService::class.java).apply {
                action = ACTION_START_STREAMING
                putExtra(EXTRA_DEVICE_ID, deviceId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun stopService(context: Context) {
            val intent = Intent(context, StreamingService::class.java).apply {
                action = ACTION_STOP_STREAMING
            }
            context.startService(intent)
        }
    }
    
    @Inject lateinit var webRTCManager: WebRTCManager
    @Inject lateinit var signalingManager: SignalingManager
    @Inject lateinit var cameraManager: CameraManager
    @Inject lateinit var screenCaptureManager: ScreenCaptureManager
    @Inject lateinit var microphoneManager: MicrophoneManager
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var deviceId: String = ""
    private var isStreaming = false
    private var currentStreamType: StreamType? = null
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "StreamingService created")
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: ${intent?.action}")
        
        when (intent?.action) {
            ACTION_START_STREAMING -> {
                deviceId = intent.getStringExtra(EXTRA_DEVICE_ID) ?: ""
                if (deviceId.isNotEmpty()) {
                    startForeground(NOTIFICATION_ID, createNotification("Waiting for stream request..."))
                    initializeAndListen()
                } else {
                    Log.e(TAG, "No device ID provided")
                    stopSelf()
                }
            }
            ACTION_STOP_STREAMING -> {
                stopStreaming()
                stopSelf()
            }
        }
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        Log.d(TAG, "StreamingService destroyed")
        stopStreaming()
        serviceScope.cancel()
        super.onDestroy()
    }
    
    /**
     * Initialize WebRTC and start listening for stream requests
     */
    private fun initializeAndListen() {
        Log.d(TAG, "Initializing streaming service for device: $deviceId")
        
        // Initialize WebRTC
        webRTCManager.initialize()
        
        // Initialize signaling
        signalingManager.initialize(deviceId)
        
        // Set up WebRTC callbacks
        setupWebRTCCallbacks()
        
        // Start listening for stream requests
        listenForStreamRequests()
        
        // Listen for answers
        listenForAnswers()
        
        // Listen for ICE candidates
        listenForIceCandidates()
    }
    
    /**
     * Set up WebRTC callbacks
     */
    private fun setupWebRTCCallbacks() {
        webRTCManager.onOfferCreated = { offer ->
            serviceScope.launch {
                try {
                    signalingManager.sendOffer(offer)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to send offer", e)
                }
            }
        }
        
        webRTCManager.onIceCandidate = { candidate ->
            serviceScope.launch {
                try {
                    signalingManager.sendIceCandidate(candidate)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to send ICE candidate", e)
                }
            }
        }
        
        webRTCManager.onConnectionStateChanged = { state ->
            serviceScope.launch {
                val status = webRTCManager.streamStatus.value
                try {
                    signalingManager.updateStreamStatus(status)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to update stream status", e)
                }
                
                // Update notification
                updateNotification(status)
            }
        }
    }
    
    /**
     * Listen for stream requests from parent
     */
    private fun listenForStreamRequests() {
        serviceScope.launch {
            signalingManager.observeStreamRequests().collectLatest { request ->
                if (request != null && request.isActive) {
                    Log.d(TAG, "Received stream request: $request")
                    handleStreamRequest(request)
                } else if (request != null && !request.isActive) {
                    Log.d(TAG, "Stream request deactivated")
                    stopStreaming()
                }
            }
        }
    }
    
    /**
     * Listen for answers from parent
     */
    private fun listenForAnswers() {
        serviceScope.launch {
            signalingManager.observeAnswer().collectLatest { answer ->
                if (answer != null) {
                    Log.d(TAG, "Received answer from parent")
                    webRTCManager.setRemoteAnswer(answer)
                }
            }
        }
    }
    
    /**
     * Listen for ICE candidates from parent
     */
    private fun listenForIceCandidates() {
        serviceScope.launch {
            signalingManager.observeParentIceCandidates().collectLatest { candidate ->
                Log.d(TAG, "Received ICE candidate from parent")
                webRTCManager.addIceCandidate(candidate)
            }
        }
    }
    
    /**
     * Handle incoming stream request
     */
    private fun handleStreamRequest(request: StreamRequest) {
        Log.d(TAG, "Handling stream request: ${request.type}")
        
        // Stop any existing stream
        if (isStreaming) {
            stopCurrentStream()
        }
        
        // Create stream config
        val config = StreamConfig.fromRequest(request)
        
        // Update notification
        updateNotification(StreamStatus.connecting(request.type))
        
        // Start streaming based on type
        when (request.type) {
            StreamType.CAMERA_FRONT, StreamType.CAMERA_BACK -> {
                startCameraStream(config, request.type)
            }
            StreamType.SCREEN -> {
                startScreenStream(config)
            }
            StreamType.AUDIO_ONLY -> {
                startAudioOnlyStream(config)
            }
        }
        
        currentStreamType = request.type
        isStreaming = true
    }
    
    /**
     * Start camera streaming
     */
    private fun startCameraStream(config: StreamConfig, cameraType: StreamType) {
        Log.d(TAG, "Starting camera stream: $cameraType")
        
        val eglContext = webRTCManager.getEglBaseContext() ?: run {
            Log.e(TAG, "EGL context not available")
            return
        }
        
        val factory = webRTCManager.getPeerConnectionFactory() ?: run {
            Log.e(TAG, "PeerConnectionFactory not available")
            return
        }
        
        // Initialize camera
        val videoTrack = cameraManager.initialize(factory, eglContext, cameraType)
        if (videoTrack != null) {
            webRTCManager.addVideoTrack(videoTrack)
            cameraManager.startCapture(config.videoConfig.let { 
                com.myparentalcontrol.shared.streaming.enums.VideoQuality.entries.find { q ->
                    q.width == it.width && q.height == it.height
                } ?: com.myparentalcontrol.shared.streaming.enums.VideoQuality.MEDIUM
            })
        }
        
        // Add audio if enabled
        if (config.audioConfig.enabled && config.audioConfig.needsMicrophone()) {
            val audioTrack = microphoneManager.initialize(factory, config.audioConfig)
            if (audioTrack != null) {
                webRTCManager.addAudioTrack(audioTrack)
            }
        }
        
        // Start WebRTC connection
        webRTCManager.startStreaming(config, deviceId)
        webRTCManager.createOffer()
    }
    
    /**
     * Start screen streaming
     */
    private fun startScreenStream(config: StreamConfig) {
        Log.d(TAG, "Starting screen stream")
        
        if (!screenCaptureManager.hasPermission()) {
            Log.e(TAG, "Screen capture permission not granted")
            serviceScope.launch {
                signalingManager.updateStreamStatus(
                    StreamStatus.error("Screen capture permission not granted")
                )
            }
            return
        }
        
        val eglContext = webRTCManager.getEglBaseContext() ?: run {
            Log.e(TAG, "EGL context not available")
            return
        }
        
        val factory = webRTCManager.getPeerConnectionFactory() ?: run {
            Log.e(TAG, "PeerConnectionFactory not available")
            return
        }
        
        // Initialize screen capture
        val videoTrack = screenCaptureManager.initialize(factory, eglContext)
        if (videoTrack != null) {
            webRTCManager.addVideoTrack(videoTrack)
            screenCaptureManager.startCapture(config.videoConfig.let {
                com.myparentalcontrol.shared.streaming.enums.VideoQuality.entries.find { q ->
                    q.width == it.width && q.height == it.height
                } ?: com.myparentalcontrol.shared.streaming.enums.VideoQuality.MEDIUM
            })
        }
        
        // Add audio if enabled
        if (config.audioConfig.enabled) {
            when (config.audioConfig.source) {
                AudioSource.MICROPHONE -> {
                    val audioTrack = microphoneManager.initialize(factory, config.audioConfig)
                    if (audioTrack != null) {
                        webRTCManager.addAudioTrack(audioTrack)
                    }
                }
                AudioSource.DEVICE_AUDIO, AudioSource.BOTH -> {
                    // Device audio requires additional setup
                    // For now, fall back to microphone
                    val audioTrack = microphoneManager.initialize(factory, config.audioConfig)
                    if (audioTrack != null) {
                        webRTCManager.addAudioTrack(audioTrack)
                    }
                }
                AudioSource.NONE -> { /* No audio */ }
            }
        }
        
        // Start WebRTC connection
        webRTCManager.startStreaming(config, deviceId)
        webRTCManager.createOffer()
    }
    
    /**
     * Start audio-only streaming
     */
    private fun startAudioOnlyStream(config: StreamConfig) {
        Log.d(TAG, "Starting audio-only stream")
        
        val factory = webRTCManager.getPeerConnectionFactory() ?: run {
            Log.e(TAG, "PeerConnectionFactory not available")
            return
        }
        
        // Initialize microphone
        val audioTrack = microphoneManager.initialize(factory, config.audioConfig)
        if (audioTrack != null) {
            webRTCManager.addAudioTrack(audioTrack)
        }
        
        // Start WebRTC connection
        webRTCManager.startStreaming(config, deviceId)
        webRTCManager.createOffer()
    }
    
    /**
     * Stop current stream
     */
    private fun stopCurrentStream() {
        Log.d(TAG, "Stopping current stream")
        
        when (currentStreamType) {
            StreamType.CAMERA_FRONT, StreamType.CAMERA_BACK -> {
                cameraManager.release()
            }
            StreamType.SCREEN -> {
                screenCaptureManager.release()
            }
            else -> {}
        }
        
        microphoneManager.release()
        webRTCManager.stopStreaming()
        
        currentStreamType = null
        isStreaming = false
    }
    
    /**
     * Stop streaming completely
     */
    private fun stopStreaming() {
        Log.d(TAG, "Stopping streaming service")
        
        stopCurrentStream()
        
        serviceScope.launch {
            try {
                signalingManager.updateStreamStatus(StreamStatus.disconnected())
                signalingManager.clearSignalingData()
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing signaling data", e)
            }
        }
        
        signalingManager.stopListening()
        webRTCManager.release()
    }
    
    /**
     * Create notification channel
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Streaming Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when streaming is active"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Create notification
     */
    private fun createNotification(message: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Parental Control")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }
    
    /**
     * Update notification based on stream status
     */
    private fun updateNotification(status: StreamStatus) {
        val message = when {
            status.isStreaming -> {
                val type = when (status.streamType) {
                    StreamType.CAMERA_FRONT -> "Front Camera"
                    StreamType.CAMERA_BACK -> "Back Camera"
                    StreamType.SCREEN -> "Screen"
                    StreamType.AUDIO_ONLY -> "Audio"
                    null -> "Unknown"
                }
                "Streaming: $type"
            }
            status.error != null -> "Error: ${status.error}"
            else -> "Waiting for stream request..."
        }
        
        val notification = createNotification(message)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
