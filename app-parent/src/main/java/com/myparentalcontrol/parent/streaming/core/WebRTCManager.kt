package com.myparentalcontrol.parent.streaming.core

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
 * WebRTC manager for the parent app
 * Receives and displays streams from child device
 */
@Singleton
class WebRTCManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "ParentWebRTCManager"
    }
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    // WebRTC components
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var eglBase: EglBase? = null
    
    // Remote tracks
    private var remoteVideoTrack: VideoTrack? = null
    private var remoteAudioTrack: AudioTrack? = null
    
    // Video renderer
    private var videoRenderer: SurfaceViewRenderer? = null
    
    // State
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    private val _streamStatus = MutableStateFlow(StreamStatus())
    val streamStatus: StateFlow<StreamStatus> = _streamStatus.asStateFlow()
    
    private val _hasVideo = MutableStateFlow(false)
    val hasVideo: StateFlow<Boolean> = _hasVideo.asStateFlow()
    
    private val _hasAudio = MutableStateFlow(false)
    val hasAudio: StateFlow<Boolean> = _hasAudio.asStateFlow()
    
    // Callbacks
    var onAnswerCreated: ((SignalingData) -> Unit)? = null
    var onIceCandidate: ((IceCandidateData) -> Unit)? = null
    var onConnectionStateChanged: ((ConnectionState) -> Unit)? = null
    var onVideoTrackReceived: ((VideoTrack) -> Unit)? = null
    var onAudioTrackReceived: ((AudioTrack) -> Unit)? = null
    
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
            true,
            true
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
     * Set up video renderer
     */
    fun setupVideoRenderer(renderer: SurfaceViewRenderer) {
        Log.d(TAG, "Setting up video renderer")
        
        videoRenderer = renderer
        renderer.init(eglBase?.eglBaseContext, null)
        renderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
        renderer.setEnableHardwareScaler(true)
        renderer.setMirror(false)
    }
    
    /**
     * Start receiving stream
     */
    fun startReceiving(config: StreamConfig, deviceId: String) {
        Log.d(TAG, "Starting to receive stream")
        this.deviceId = deviceId
        
        _connectionState.value = ConnectionState.CONNECTING
        
        createPeerConnection(config)
    }
    
    /**
     * Create peer connection
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
        
        // Add transceivers for receiving
        peerConnection?.addTransceiver(
            MediaStreamTrack.MediaType.MEDIA_TYPE_VIDEO,
            RtpTransceiver.RtpTransceiverInit(RtpTransceiver.RtpTransceiverDirection.RECV_ONLY)
        )
        peerConnection?.addTransceiver(
            MediaStreamTrack.MediaType.MEDIA_TYPE_AUDIO,
            RtpTransceiver.RtpTransceiverInit(RtpTransceiver.RtpTransceiverDirection.RECV_ONLY)
        )
        
        Log.d(TAG, "Peer connection created")
    }
    
    /**
     * Set remote offer from child and create answer
     */
    fun setRemoteOffer(offer: SignalingData) {
        Log.d(TAG, "Setting remote offer")
        
        val sdp = SessionDescription(SessionDescription.Type.OFFER, offer.sdp)
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onSetSuccess() {
                Log.d(TAG, "Remote description set, creating answer")
                createAnswer()
            }
            override fun onCreateFailure(error: String?) {
                Log.e(TAG, "Failed to create: $error")
            }
            override fun onSetFailure(error: String?) {
                Log.e(TAG, "Failed to set remote description: $error")
            }
        }, sdp)
    }
    
    /**
     * Create answer
     */
    private fun createAnswer() {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }
        
        peerConnection?.createAnswer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription) {
                Log.d(TAG, "Answer created successfully")
                peerConnection?.setLocalDescription(object : SdpObserver {
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onSetSuccess() {
                        Log.d(TAG, "Local description set")
                        val answer = SignalingData.answer(sdp.description, deviceId)
                        onAnswerCreated?.invoke(answer)
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
                Log.e(TAG, "Failed to create answer: $error")
            }
            override fun onSetFailure(error: String?) {}
        }, constraints)
    }
    
    /**
     * Add ICE candidate from child
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
                        }
                        PeerConnection.IceConnectionState.DISCONNECTED -> {
                            _connectionState.value = ConnectionState.DISCONNECTED
                        }
                        PeerConnection.IceConnectionState.FAILED -> {
                            _connectionState.value = ConnectionState.FAILED
                        }
                        PeerConnection.IceConnectionState.CLOSED -> {
                            _connectionState.value = ConnectionState.CLOSED
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
                    Log.d(TAG, "New ICE candidate")
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
                Log.d(TAG, "Stream added: ${stream?.id}")
                stream?.let { handleRemoteStream(it) }
            }
            
            override fun onRemoveStream(stream: MediaStream?) {
                Log.d(TAG, "Stream removed")
                scope.launch {
                    _hasVideo.value = false
                    _hasAudio.value = false
                }
            }
            
            override fun onDataChannel(channel: DataChannel?) {
                Log.d(TAG, "Data channel created")
            }
            
            override fun onRenegotiationNeeded() {
                Log.d(TAG, "Renegotiation needed")
            }
            
            override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {
                Log.d(TAG, "Track added: ${receiver?.track()?.kind()}")
                receiver?.track()?.let { track ->
                    when (track) {
                        is VideoTrack -> {
                            remoteVideoTrack = track
                            track.setEnabled(true)
                            videoRenderer?.let { track.addSink(it) }
                            scope.launch {
                                _hasVideo.value = true
                            }
                            onVideoTrackReceived?.invoke(track)
                        }
                        is AudioTrack -> {
                            remoteAudioTrack = track
                            track.setEnabled(true)
                            scope.launch {
                                _hasAudio.value = true
                            }
                            onAudioTrackReceived?.invoke(track)
                        }
                        else -> {
                            Log.d(TAG, "Unknown track type: ${track.kind()}")
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Handle remote stream
     */
    private fun handleRemoteStream(stream: MediaStream) {
        Log.d(TAG, "Handling remote stream with ${stream.videoTracks.size} video tracks and ${stream.audioTracks.size} audio tracks")
        
        // Handle video tracks
        if (stream.videoTracks.isNotEmpty()) {
            remoteVideoTrack = stream.videoTracks[0]
            remoteVideoTrack?.setEnabled(true)
            videoRenderer?.let { remoteVideoTrack?.addSink(it) }
            scope.launch {
                _hasVideo.value = true
            }
            remoteVideoTrack?.let { onVideoTrackReceived?.invoke(it) }
        }
        
        // Handle audio tracks
        if (stream.audioTracks.isNotEmpty()) {
            remoteAudioTrack = stream.audioTracks[0]
            remoteAudioTrack?.setEnabled(true)
            scope.launch {
                _hasAudio.value = true
            }
            remoteAudioTrack?.let { onAudioTrackReceived?.invoke(it) }
        }
    }
    
    /**
     * Mute/unmute audio
     */
    fun setAudioEnabled(enabled: Boolean) {
        remoteAudioTrack?.setEnabled(enabled)
        Log.d(TAG, "Audio enabled: $enabled")
    }
    
    /**
     * Enable/disable video
     */
    fun setVideoEnabled(enabled: Boolean) {
        remoteVideoTrack?.setEnabled(enabled)
        Log.d(TAG, "Video enabled: $enabled")
    }
    
    /**
     * Stop receiving stream
     */
    fun stopReceiving() {
        Log.d(TAG, "Stopping stream reception")
        
        remoteVideoTrack?.let { track ->
            videoRenderer?.let { track.removeSink(it) }
        }
        remoteVideoTrack = null
        remoteAudioTrack = null
        
        peerConnection?.close()
        peerConnection = null
        
        _connectionState.value = ConnectionState.DISCONNECTED
        _hasVideo.value = false
        _hasAudio.value = false
        
        Log.d(TAG, "Stream reception stopped")
    }
    
    /**
     * Release video renderer
     */
    fun releaseVideoRenderer() {
        Log.d(TAG, "Releasing video renderer")
        
        remoteVideoTrack?.let { track ->
            videoRenderer?.let { track.removeSink(it) }
        }
        
        videoRenderer?.release()
        videoRenderer = null
    }
    
    /**
     * Release all resources
     */
    fun release() {
        Log.d(TAG, "Releasing WebRTC resources")
        
        stopReceiving()
        releaseVideoRenderer()
        
        peerConnectionFactory?.dispose()
        peerConnectionFactory = null
        
        eglBase?.release()
        eglBase = null
        
        Log.d(TAG, "WebRTC resources released")
    }
    
    /**
     * Get EGL base context
     */
    fun getEglBaseContext(): EglBase.Context? = eglBase?.eglBaseContext
}
