package com.myparentalcontrol.child.streaming.video

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import com.myparentalcontrol.shared.streaming.enums.VideoQuality
import dagger.hilt.android.qualifiers.ApplicationContext
import org.webrtc.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages screen capture for WebRTC streaming
 * Requires MediaProjection permission from user
 */
@Singleton
class ScreenCaptureManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "ScreenCaptureManager"
        const val SCREEN_CAPTURE_REQUEST_CODE = 1001
    }
    
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var screenCapturer: ScreenCapturerAndroid? = null
    private var videoSource: VideoSource? = null
    private var videoTrack: VideoTrack? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null
    
    private var isCapturing = false
    private var mediaProjectionIntent: Intent? = null
    private var mediaProjectionResultCode: Int = Activity.RESULT_CANCELED
    
    // Screen dimensions
    private var screenWidth: Int = 1280
    private var screenHeight: Int = 720
    private var screenDensity: Int = 1
    
    /**
     * Request screen capture permission
     * Call this from an Activity to get MediaProjection permission
     */
    fun requestScreenCapturePermission(activity: Activity) {
        Log.d(TAG, "Requesting screen capture permission")
        
        val mediaProjectionManager = activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE) 
            as MediaProjectionManager
        activity.startActivityForResult(
            mediaProjectionManager.createScreenCaptureIntent(),
            SCREEN_CAPTURE_REQUEST_CODE
        )
    }
    
    /**
     * Handle permission result from Activity
     */
    fun handlePermissionResult(resultCode: Int, data: Intent?) {
        Log.d(TAG, "Permission result: $resultCode")
        
        if (resultCode == Activity.RESULT_OK && data != null) {
            mediaProjectionResultCode = resultCode
            mediaProjectionIntent = data
            Log.d(TAG, "Screen capture permission granted")
        } else {
            Log.e(TAG, "Screen capture permission denied")
        }
    }
    
    /**
     * Check if screen capture permission is granted
     */
    fun hasPermission(): Boolean = mediaProjectionIntent != null
    
    /**
     * Initialize screen capturer
     */
    fun initialize(
        peerConnectionFactory: PeerConnectionFactory,
        eglBaseContext: EglBase.Context
    ): VideoTrack? {
        Log.d(TAG, "Initializing screen capture")
        
        if (mediaProjectionIntent == null) {
            Log.e(TAG, "No screen capture permission")
            return null
        }
        
        // Get screen dimensions
        updateScreenDimensions()
        
        // Create media projection callback
        val mediaProjectionCallback = object : MediaProjection.Callback() {
            override fun onStop() {
                Log.d(TAG, "MediaProjection stopped")
                stopCapture()
            }
        }
        
        // Create screen capturer
        screenCapturer = ScreenCapturerAndroid(
            mediaProjectionIntent,
            mediaProjectionCallback
        )
        
        // Create surface texture helper
        surfaceTextureHelper = SurfaceTextureHelper.create(
            "ScreenCaptureThread",
            eglBaseContext
        )
        
        // Create video source
        videoSource = peerConnectionFactory.createVideoSource(true) // isScreencast = true
        screenCapturer?.initialize(
            surfaceTextureHelper,
            context,
            videoSource?.capturerObserver
        )
        
        // Create video track
        videoTrack = peerConnectionFactory.createVideoTrack("screen_track", videoSource)
        videoTrack?.setEnabled(true)
        
        Log.d(TAG, "Screen capture initialized successfully")
        return videoTrack
    }
    
    /**
     * Start screen capture with specified quality
     */
    fun startCapture(quality: VideoQuality = VideoQuality.MEDIUM) {
        if (isCapturing) {
            Log.w(TAG, "Screen capture is already running")
            return
        }
        
        Log.d(TAG, "Starting screen capture with quality: $quality")
        
        try {
            // Adjust dimensions based on quality while maintaining aspect ratio
            val captureWidth: Int
            val captureHeight: Int
            
            val aspectRatio = screenWidth.toFloat() / screenHeight.toFloat()
            if (aspectRatio > 1) {
                // Landscape
                captureWidth = quality.width
                captureHeight = (quality.width / aspectRatio).toInt()
            } else {
                // Portrait
                captureHeight = quality.height
                captureWidth = (quality.height * aspectRatio).toInt()
            }
            
            screenCapturer?.startCapture(
                captureWidth,
                captureHeight,
                quality.frameRate
            )
            isCapturing = true
            Log.d(TAG, "Screen capture started: ${captureWidth}x${captureHeight}@${quality.frameRate}fps")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start screen capture", e)
        }
    }
    
    /**
     * Stop screen capture
     */
    fun stopCapture() {
        if (!isCapturing) {
            Log.w(TAG, "Screen capture is not running")
            return
        }
        
        Log.d(TAG, "Stopping screen capture")
        
        try {
            screenCapturer?.stopCapture()
            isCapturing = false
            Log.d(TAG, "Screen capture stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop screen capture", e)
        }
    }
    
    /**
     * Change capture quality
     */
    fun changeQuality(quality: VideoQuality) {
        Log.d(TAG, "Changing quality to: $quality")
        
        val aspectRatio = screenWidth.toFloat() / screenHeight.toFloat()
        val captureWidth: Int
        val captureHeight: Int
        
        if (aspectRatio > 1) {
            captureWidth = quality.width
            captureHeight = (quality.width / aspectRatio).toInt()
        } else {
            captureHeight = quality.height
            captureWidth = (quality.height * aspectRatio).toInt()
        }
        
        screenCapturer?.changeCaptureFormat(
            captureWidth,
            captureHeight,
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
     * Check if screen capture is running
     */
    fun isCapturing(): Boolean = isCapturing
    
    /**
     * Release all resources
     */
    fun release() {
        Log.d(TAG, "Releasing screen capture resources")
        
        stopCapture()
        
        videoTrack?.dispose()
        videoTrack = null
        
        videoSource?.dispose()
        videoSource = null
        
        screenCapturer?.dispose()
        screenCapturer = null
        
        surfaceTextureHelper?.dispose()
        surfaceTextureHelper = null
        
        virtualDisplay?.release()
        virtualDisplay = null
        
        mediaProjection?.stop()
        mediaProjection = null
        
        // Don't clear the intent so we can reuse the permission
        // mediaProjectionIntent = null
        
        Log.d(TAG, "Screen capture resources released")
    }
    
    /**
     * Update screen dimensions from display metrics
     */
    private fun updateScreenDimensions() {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val displayMetrics = DisplayMetrics()
        
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealMetrics(displayMetrics)
        
        screenWidth = displayMetrics.widthPixels
        screenHeight = displayMetrics.heightPixels
        screenDensity = displayMetrics.densityDpi
        
        Log.d(TAG, "Screen dimensions: ${screenWidth}x${screenHeight}, density: $screenDensity")
    }
    
    /**
     * Get screen width
     */
    fun getScreenWidth(): Int = screenWidth
    
    /**
     * Get screen height
     */
    fun getScreenHeight(): Int = screenHeight
}
