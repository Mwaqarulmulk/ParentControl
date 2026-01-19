package com.myparentalcontrol.child.streaming.core

import android.content.Context
import android.util.Log
import com.myparentalcontrol.shared.streaming.enums.ConnectionState
import com.myparentalcontrol.shared.streaming.models.IceCandidateData
import com.myparentalcontrol.shared.streaming.models.SignalingData
import com.myparentalcontrol.shared.streaming.models.StreamConfig
import com.myparentalcontrol.shared.streaming.models.StreamStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.webrtc.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Main WebRTC manager for the child app
 * Handles peer connection creation, media tracks, and signaling
 */
@Singleton
class WebRTCManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "WebRTCManager"
    }
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    // WebRTC components
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var eglBase: EglBase? = null
    
    // Media tracks
    private var localVideoTrack: VideoTrack? = null
    private var localAudioTrack: AudioTrack? = null
    private var videoCapturer: VideoCapturer? = null
    private var videoSource: VideoSource? = null
    private var audioSource: AudioSource? = null
    
    // State
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    private val _streamStatus = MutableStateFlow(StreamStatus())
    val streamStatus: StateFlow<StreamStatus> = _streamStatus.asStateFlow()
    
    // Callbacks
    var onIceCandidate: ((IceCandidateData) -> Unit)? = null
    var onOfferCreated: ((SignalingData) -> Unit)? = null
    var onConnectionStateChanged: ((ConnectionState) -> Unit)? = null
    
    private var currentConfig: StreamConfig? = null
    private var deviceId: String = ""
    
    /**
     * Initialize WebRTC
     */
    fun initialize() {
        Log.d(TAG, "Initializing WebRTC")
        
        // Initialize EGL context for video rendering
        eglBase = EglBase.create()
        
        // Initialize PeerConnectionFactory
        val options = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(true)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)
        
        val encoderFactory = DefaultVideoEncoderFactory(
            eglBase?.eglBaseContext,
            true, // enableIntelVp8Encoder
            true  // enableH264HighProfile
        )
        
        val decoderFactory = DefaultVideoDecoderFactory(eglBase?.eglBaseContext)
        
        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .setOptions(PeerConnectionFactory.Options())
            .createPeerConnectionFactory()
        
        Log.d(TAG, "WebRTC initialized successfully")
    }
    
    /**
     * Start streaming with the given configuration
     */
    fun startStreaming(config: StreamConfig, deviceId: String) {
        Log.d(TAG, "Starting streaming with config: $config")
        this.currentConfig = config
        this.deviceId = deviceId
        
        _connectionState.value = ConnectionState.CONNECTING
        _streamStatus.value = StreamStatus.connecting(config.streamType)
        
        createPeerConnection(config)
    }
    
    /**
     * Create peer connection with ICE servers
     */
    private fun createPeerConnection(config: StreamConfig) {
        val iceServers = config.iceServers.map { server ->
            PeerConnection.IceServer.builder(server.urls)
                .apply {
                    server.username?.let { setUsername(it) }
                    server.credential?.let { setPassword(it) }
                }
                .createIceServer()
        }
        
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
            rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
            tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
            keyType = PeerConnection.KeyType.ECDSA
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }
        
        peerConnection = peerConnectionFactory?.createPeerConnection(
            rtcConfig,
            createPeerConnectionObserver()
        )
        
        Log.d(TAG, "Peer connection created")
    }
    
    /**
     * Add video track to peer connection
     */
    fun addVideoTrack(videoTrack: VideoTrack) {
        Log.d(TAG, "Adding video track")
        localVideoTrack = videoTrack
        peerConnection?.addTrack(videoTrack, listOf("stream"))
    }
    
    /**
     * Add audio track to peer connection
     */
    fun addAudioTrack(audioTrack: AudioTrack) {
        Log.d(TAG, "Adding audio track")
        localAudioTrack = audioTrack
        peerConnection?.addTrack(audioTrack, listOf("stream"))
    }
    
    /**
     * Create and send offer
     */
    fun createOffer() {
        Log.d(TAG, "Creating offer")
        
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "false"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"))
        }
        
        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription) {
                Log.d(TAG, "Offer created successfully")
                peerConnection?.setLocalDescription(object : SdpObserver {
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onSetSuccess() {
                        Log.d(TAG, "Local description set")
                        val offer = SignalingData.offer(sdp.description, deviceId)
                        onOfferCreated?.invoke(offer)
                    }
                    override fun onCreateFailure(error: String?) {
                        Log.e(TAG, "Failed to create local description: $error")
                    }
                    override fun onSetFailure(error: String?) {
                        Log.e(TAG, "Failed to set local description: $error")
                    }
                }, sdp)
            }
            
            override fun onSetSuccess() {}
            override fun onCreateFailure(error: String?) {
                Log.e(TAG, "Failed to create offer: $error")
                _connectionState.value = ConnectionState.FAILED
            }
            override fun onSetFailure(error: String?) {}
        }, constraints)
    }
    
    /**
     * Set remote answer from parent
     */
    fun setRemoteAnswer(answer: SignalingData) {
        Log.d(TAG, "Setting remote answer")
        
        val sdp = SessionDescription(SessionDescription.Type.ANSWER, answer.sdp)
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onSetSuccess() {
                Log.d(TAG, "Remote description set successfully")
            }
            override fun onCreateFailure(error: String?) {}
            override fun onSetFailure(error: String?) {
                Log.e(TAG, "Failed to set remote description: $error")
            }
        }, sdp)
    }
    
    /**
     * Add ICE candidate from parent
     */
    fun addIceCandidate(candidate: IceCandidateData) {
        Log.d(TAG, "Adding ICE candidate")
        
        val iceCandidate = IceCandidate(
            candidate.sdpMid,
            candidate.sdpMLineIndex,
            candidate.candidate
        )
        peerConnection?.addIceCandidate(iceCandidate)
    }
    
    /**
     * Create peer connection observer
     */
    private fun createPeerConnectionObserver(): PeerConnection.Observer {
        return object : PeerConnection.Observer {
            override fun onSignalingChange(state: PeerConnection.SignalingState?) {
                Log.d(TAG, "Signaling state changed: $state")
            }
            
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                Log.d(TAG, "ICE connection state changed: $state")
                scope.launch {
                    when (state) {
                        PeerConnection.IceConnectionState.CONNECTED -> {
                            _connectionState.value = ConnectionState.CONNECTED
                            currentConfig?.let { config ->
                                _streamStatus.value = StreamStatus.streaming(
                                    streamType = config.streamType,
                                    audioEnabled = config.audioConfig.enabled,
                                    audioSource = config.audioConfig.source
                                )
                            }
                        }
                        PeerConnection.IceConnectionState.DISCONNECTED -> {
                            _connectionState.value = ConnectionState.DISCONNECTED
                            _streamStatus.value = StreamStatus.disconnected()
                        }
                        PeerConnection.IceConnectionState.FAILED -> {
                            _connectionState.value = ConnectionState.FAILED
                            _streamStatus.value = StreamStatus.error("ICE connection failed")
                        }
                        PeerConnection.IceConnectionState.CLOSED -> {
                            _connectionState.value = ConnectionState.CLOSED
                            _streamStatus.value = StreamStatus.disconnected()
                        }
                        else -> {}
                    }
                    onConnectionStateChanged?.invoke(_connectionState.value)
                }
            }
            
            override fun onIceConnectionReceivingChange(receiving: Boolean) {
                Log.d(TAG, "ICE connection receiving: $receiving")
            }
            
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {
                Log.d(TAG, "ICE gathering state changed: $state")
            }
            
            override fun onIceCandidate(candidate: IceCandidate?) {
                candidate?.let {
                    Log.d(TAG, "New ICE candidate: ${it.sdp}")
                    val candidateData = IceCandidateData(
                        sdpMid = it.sdpMid,
                        sdpMLineIndex = it.sdpMLineIndex,
                        candidate = it.sdp,
                        senderId = deviceId
                    )
                    onIceCandidate?.invoke(candidateData)
                }
            }
            
            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {
                Log.d(TAG, "ICE candidates removed")
            }
            
            override fun onAddStream(stream: MediaStream?) {
                Log.d(TAG, "Stream added")
            }
            
            override fun onRemoveStream(stream: MediaStream?) {
                Log.d(TAG, "Stream removed")
            }
            
            override fun onDataChannel(channel: DataChannel?) {
                Log.d(TAG, "Data channel created")
            }
            
            override fun onRenegotiationNeeded() {
                Log.d(TAG, "Renegotiation needed")
            }
            
            override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {
                Log.d(TAG, "Track added")
            }
        }
    }
    
    /**
     * Stop streaming and clean up
     */
    fun stopStreaming() {
        Log.d(TAG, "Stopping streaming")
        
        localVideoTrack?.setEnabled(false)
        localAudioTrack?.setEnabled(false)
        
        videoCapturer?.stopCapture()
        videoCapturer?.dispose()
        videoCapturer = null
        
        videoSource?.dispose()
        videoSource = null
        
        audioSource?.dispose()
        audioSource = null
        
        localVideoTrack?.dispose()
        localVideoTrack = null
        
        localAudioTrack?.dispose()
        localAudioTrack = null
        
        peerConnection?.close()
        peerConnection = null
        
        _connectionState.value = ConnectionState.DISCONNECTED
        _streamStatus.value = StreamStatus.disconnected()
        
        Log.d(TAG, "Streaming stopped")
    }
    
    /**
     * Release all resources
     */
    fun release() {
        Log.d(TAG, "Releasing WebRTC resources")
        
        stopStreaming()
        
        peerConnectionFactory?.dispose()
        peerConnectionFactory = null
        
        eglBase?.release()
        eglBase = null
        
        Log.d(TAG, "WebRTC resources released")
    }
    
    /**
     * Get EGL base context for video rendering
     */
    fun getEglBaseContext(): EglBase.Context? = eglBase?.eglBaseContext
    
    /**
     * Get peer connection factory
     */
    fun getPeerConnectionFactory(): PeerConnectionFactory? = peerConnectionFactory
}
