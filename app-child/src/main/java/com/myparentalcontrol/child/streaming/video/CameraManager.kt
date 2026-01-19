package com.myparentalcontrol.child.streaming.video

import android.content.Context
import android.util.Log
import com.myparentalcontrol.shared.streaming.enums.StreamType
import com.myparentalcontrol.shared.streaming.enums.VideoQuality
import dagger.hilt.android.qualifiers.ApplicationContext
import org.webrtc.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages camera capture for WebRTC streaming
 */
@Singleton
class CameraManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "CameraManager"
    }
    
    private var videoCapturer: CameraVideoCapturer? = null
    private var videoSource: VideoSource? = null
    private var videoTrack: VideoTrack? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null
    
    private var isCapturing = false
    private var currentCameraType: StreamType = StreamType.CAMERA_FRONT
    
    /**
     * Initialize camera capturer
     */
    fun initialize(
        peerConnectionFactory: PeerConnectionFactory,
        eglBaseContext: EglBase.Context,
        cameraType: StreamType = StreamType.CAMERA_FRONT
    ): VideoTrack? {
        Log.d(TAG, "Initializing camera: $cameraType")
        
        currentCameraType = cameraType
        
        // Create camera capturer
        videoCapturer = createCameraCapturer(cameraType)
        if (videoCapturer == null) {
            Log.e(TAG, "Failed to create camera capturer")
            return null
        }
        
        // Create surface texture helper
        surfaceTextureHelper = SurfaceTextureHelper.create(
            "CaptureThread",
            eglBaseContext
        )
        
        // Create video source
        videoSource = peerConnectionFactory.createVideoSource(videoCapturer!!.isScreencast)
        videoCapturer?.initialize(
            surfaceTextureHelper,
            context,
            videoSource?.capturerObserver
        )
        
        // Create video track
        videoTrack = peerConnectionFactory.createVideoTrack("video_track", videoSource)
        videoTrack?.setEnabled(true)
        
        Log.d(TAG, "Camera initialized successfully")
        return videoTrack
    }
    
    /**
     * Start camera capture with specified quality
     */
    fun startCapture(quality: VideoQuality = VideoQuality.MEDIUM) {
        if (isCapturing) {
            Log.w(TAG, "Camera is already capturing")
            return
        }
        
        Log.d(TAG, "Starting camera capture with quality: $quality")
        
        try {
            videoCapturer?.startCapture(
                quality.width,
                quality.height,
                quality.frameRate
            )
            isCapturing = true
            Log.d(TAG, "Camera capture started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start camera capture", e)
        }
    }
    
    /**
     * Stop camera capture
     */
    fun stopCapture() {
        if (!isCapturing) {
            Log.w(TAG, "Camera is not capturing")
            return
        }
        
        Log.d(TAG, "Stopping camera capture")
        
        try {
            videoCapturer?.stopCapture()
            isCapturing = false
            Log.d(TAG, "Camera capture stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop camera capture", e)
        }
    }
    
    /**
     * Switch between front and back camera
     */
    fun switchCamera() {
        Log.d(TAG, "Switching camera")
        
        (videoCapturer as? CameraVideoCapturer)?.switchCamera(object : CameraVideoCapturer.CameraSwitchHandler {
            override fun onCameraSwitchDone(isFrontCamera: Boolean) {
                currentCameraType = if (isFrontCamera) StreamType.CAMERA_FRONT else StreamType.CAMERA_BACK
                Log.d(TAG, "Camera switched to: $currentCameraType")
            }
            
            override fun onCameraSwitchError(error: String?) {
                Log.e(TAG, "Failed to switch camera: $error")
            }
        })
    }
    
    /**
     * Change capture quality
     */
    fun changeQuality(quality: VideoQuality) {
        Log.d(TAG, "Changing quality to: $quality")
        
        videoCapturer?.changeCaptureFormat(
            quality.width,
            quality.height,
            quality.frameRate
        )
    }
    
    /**
     * Enable/disable video track
     */
    fun setEnabled(enabled: Boolean) {
        videoTrack?.setEnabled(enabled)
        Log.d(TAG, "Video track enabled: $enabled")
    }
    
    /**
     * Get current video track
     */
    fun getVideoTrack(): VideoTrack? = videoTrack
    
    /**
     * Check if camera is capturing
     */
    fun isCapturing(): Boolean = isCapturing
    
    /**
     * Get current camera type
     */
    fun getCurrentCameraType(): StreamType = currentCameraType
    
    /**
     * Release all resources
     */
    fun release() {
        Log.d(TAG, "Releasing camera resources")
        
        stopCapture()
        
        videoTrack?.dispose()
        videoTrack = null
        
        videoSource?.dispose()
        videoSource = null
        
        videoCapturer?.dispose()
        videoCapturer = null
        
        surfaceTextureHelper?.dispose()
        surfaceTextureHelper = null
        
        Log.d(TAG, "Camera resources released")
    }
    
    /**
     * Create camera capturer based on camera type
     */
    private fun createCameraCapturer(cameraType: StreamType): CameraVideoCapturer? {
        val enumerator = if (Camera2Enumerator.isSupported(context)) {
            Camera2Enumerator(context)
        } else {
            Camera1Enumerator(true)
        }
        
        val deviceNames = enumerator.deviceNames
        val isFrontCamera = cameraType == StreamType.CAMERA_FRONT
        
        // First, try to find the requested camera
        for (deviceName in deviceNames) {
            if (isFrontCamera && enumerator.isFrontFacing(deviceName)) {
                val capturer = enumerator.createCapturer(deviceName, null)
                if (capturer != null) {
                    Log.d(TAG, "Using front camera: $deviceName")
                    return capturer
                }
            } else if (!isFrontCamera && enumerator.isBackFacing(deviceName)) {
                val capturer = enumerator.createCapturer(deviceName, null)
                if (capturer != null) {
                    Log.d(TAG, "Using back camera: $deviceName")
                    return capturer
                }
            }
        }
        
        // Fallback: use any available camera
        for (deviceName in deviceNames) {
            val capturer = enumerator.createCapturer(deviceName, null)
            if (capturer != null) {
                Log.d(TAG, "Using fallback camera: $deviceName")
                return capturer
            }
        }
        
        Log.e(TAG, "No camera found")
        return null
    }
    
    /**
     * Check if device has front camera
     */
    fun hasFrontCamera(): Boolean {
        val enumerator = if (Camera2Enumerator.isSupported(context)) {
            Camera2Enumerator(context)
        } else {
            Camera1Enumerator(true)
        }
        
        return enumerator.deviceNames.any { enumerator.isFrontFacing(it) }
    }
    
    /**
     * Check if device has back camera
     */
    fun hasBackCamera(): Boolean {
        val enumerator = if (Camera2Enumerator.isSupported(context)) {
            Camera2Enumerator(context)
        } else {
            Camera1Enumerator(true)
        }
        
        return enumerator.deviceNames.any { enumerator.isBackFacing(it) }
    }
}
