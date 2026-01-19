package com.myparentalcontrol.child.streaming.audio

import android.annotation.TargetApi
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.projection.MediaProjection
import android.os.Build
import android.util.Log
import com.myparentalcontrol.shared.streaming.models.AudioQuality
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages device audio capture for WebRTC streaming
 * Captures audio from apps (music, videos, games, etc.)
 * Requires Android 10+ (API 29) and MediaProjection permission
 */
@Singleton
class DeviceAudioManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "DeviceAudioManager"
        private const val SAMPLE_RATE = 44100
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_STEREO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }
    
    private var audioRecord: AudioRecord? = null
    private var mediaProjection: MediaProjection? = null
    private var isCapturing = false
    private var captureJob: Job? = null
    
    // Custom audio track for device audio (WebRTC)
    private var webRtcAudioTrack: org.webrtc.AudioTrack? = null
    private var webRtcAudioSource: org.webrtc.AudioSource? = null
    
    // Audio callback for WebRTC
    private var audioCallback: ((ByteArray, Int) -> Unit)? = null
    
    /**
     * Check if device audio capture is supported (Android 10+)
     */
    fun isSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    
    /**
     * Initialize device audio capture with MediaProjection
     * Must be called after obtaining MediaProjection permission
     */
    @TargetApi(Build.VERSION_CODES.Q)
    fun initialize(
        mediaProjection: MediaProjection,
        quality: AudioQuality = AudioQuality.MEDIUM
    ): Boolean {
        if (!isSupported()) {
            Log.e(TAG, "Device audio capture not supported on this Android version")
            return false
        }
        
        Log.d(TAG, "Initializing device audio capture")
        
        this.mediaProjection = mediaProjection
        
        try {
            // Create AudioPlaybackCaptureConfiguration
            val config = AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
                .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                .addMatchingUsage(AudioAttributes.USAGE_GAME)
                .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                .build()
            
            // Calculate buffer size
            val bufferSize = AudioRecord.getMinBufferSize(
                quality.sampleRate,
                CHANNEL_CONFIG,
                AUDIO_FORMAT
            ) * 2
            
            // Create AudioRecord with playback capture
            audioRecord = AudioRecord.Builder()
                .setAudioPlaybackCaptureConfig(config)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AUDIO_FORMAT)
                        .setSampleRate(quality.sampleRate)
                        .setChannelMask(CHANNEL_CONFIG)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .build()
            
            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "Failed to initialize AudioRecord")
                release()
                return false
            }
            
            Log.d(TAG, "Device audio capture initialized successfully")
            return true
            
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing device audio capture", e)
            release()
            return false
        }
    }
    
    /**
     * Start capturing device audio
     */
    fun startCapture(callback: (ByteArray, Int) -> Unit) {
        if (isCapturing) {
            Log.w(TAG, "Device audio capture is already running")
            return
        }
        
        if (audioRecord == null) {
            Log.e(TAG, "AudioRecord not initialized")
            return
        }
        
        Log.d(TAG, "Starting device audio capture")
        
        audioCallback = callback
        isCapturing = true
        
        audioRecord?.startRecording()
        
        // Start capture loop in coroutine
        captureJob = CoroutineScope(Dispatchers.IO).launch {
            val bufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT
            )
            val buffer = ByteArray(bufferSize)
            
            while (isActive && isCapturing) {
                val bytesRead = audioRecord?.read(buffer, 0, bufferSize) ?: 0
                
                if (bytesRead > 0) {
                    audioCallback?.invoke(buffer.copyOf(bytesRead), bytesRead)
                }
            }
        }
        
        Log.d(TAG, "Device audio capture started")
    }
    
    /**
     * Stop capturing device audio
     */
    fun stopCapture() {
        if (!isCapturing) {
            Log.w(TAG, "Device audio capture is not running")
            return
        }
        
        Log.d(TAG, "Stopping device audio capture")
        
        isCapturing = false
        captureJob?.cancel()
        captureJob = null
        
        audioRecord?.stop()
        audioCallback = null
        
        Log.d(TAG, "Device audio capture stopped")
    }
    
    /**
     * Check if device audio is being captured
     */
    fun isCapturing(): Boolean = isCapturing
    
    /**
     * Release all resources
     */
    fun release() {
        Log.d(TAG, "Releasing device audio resources")
        
        stopCapture()
        
        audioRecord?.release()
        audioRecord = null
        
        webRtcAudioTrack?.dispose()
        webRtcAudioTrack = null
        
        webRtcAudioSource?.dispose()
        webRtcAudioSource = null
        
        mediaProjection = null
        
        Log.d(TAG, "Device audio resources released")
    }
    
    /**
     * Get audio format info
     */
    fun getAudioFormat(): AudioFormatInfo {
        return AudioFormatInfo(
            sampleRate = SAMPLE_RATE,
            channelCount = 2, // Stereo
            bitsPerSample = 16
        )
    }
}

/**
 * Audio format information
 */
data class AudioFormatInfo(
    val sampleRate: Int,
    val channelCount: Int,
    val bitsPerSample: Int
) {
    val bytesPerSample: Int get() = bitsPerSample / 8
    val bytesPerFrame: Int get() = bytesPerSample * channelCount
}
