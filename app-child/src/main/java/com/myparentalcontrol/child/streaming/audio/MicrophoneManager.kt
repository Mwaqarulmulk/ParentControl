package com.myparentalcontrol.child.streaming.audio

import android.content.Context
import android.util.Log
import com.myparentalcontrol.shared.streaming.models.AudioConfig
import com.myparentalcontrol.shared.streaming.models.AudioQuality
import dagger.hilt.android.qualifiers.ApplicationContext
import org.webrtc.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages microphone audio capture for WebRTC streaming
 */
@Singleton
class MicrophoneManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "MicrophoneManager"
    }
    
    private var audioSource: AudioSource? = null
    private var audioTrack: AudioTrack? = null
    private var audioConstraints: MediaConstraints? = null
    
    private var isCapturing = false
    
    /**
     * Initialize microphone audio capture
     */
    fun initialize(
        peerConnectionFactory: PeerConnectionFactory,
        config: AudioConfig = AudioConfig()
    ): AudioTrack? {
        Log.d(TAG, "Initializing microphone with config: $config")
        
        // Create audio constraints
        audioConstraints = createAudioConstraints(config)
        
        // Create audio source
        audioSource = peerConnectionFactory.createAudioSource(audioConstraints)
        
        // Create audio track
        audioTrack = peerConnectionFactory.createAudioTrack("audio_track", audioSource)
        audioTrack?.setEnabled(true)
        
        isCapturing = true
        Log.d(TAG, "Microphone initialized successfully")
        return audioTrack
    }
    
    /**
     * Create audio constraints based on config
     */
    private fun createAudioConstraints(config: AudioConfig): MediaConstraints {
        return MediaConstraints().apply {
            // Echo cancellation
            mandatory.add(
                MediaConstraints.KeyValuePair(
                    "googEchoCancellation",
                    config.echoCancellation.toString()
                )
            )
            mandatory.add(
                MediaConstraints.KeyValuePair(
                    "googEchoCancellation2",
                    config.echoCancellation.toString()
                )
            )
            
            // Noise suppression
            mandatory.add(
                MediaConstraints.KeyValuePair(
                    "googNoiseSuppression",
                    config.noiseSuppression.toString()
                )
            )
            mandatory.add(
                MediaConstraints.KeyValuePair(
                    "googNoiseSuppression2",
                    config.noiseSuppression.toString()
                )
            )
            
            // Auto gain control
            mandatory.add(
                MediaConstraints.KeyValuePair(
                    "googAutoGainControl",
                    config.autoGainControl.toString()
                )
            )
            mandatory.add(
                MediaConstraints.KeyValuePair(
                    "googAutoGainControl2",
                    config.autoGainControl.toString()
                )
            )
            
            // High pass filter
            mandatory.add(
                MediaConstraints.KeyValuePair(
                    "googHighpassFilter",
                    "true"
                )
            )
            
            // Typing noise detection
            mandatory.add(
                MediaConstraints.KeyValuePair(
                    "googTypingNoiseDetection",
                    "true"
                )
            )
        }
    }
    
    /**
     * Enable/disable audio track
     */
    fun setEnabled(enabled: Boolean) {
        audioTrack?.setEnabled(enabled)
        Log.d(TAG, "Audio track enabled: $enabled")
    }
    
    /**
     * Mute/unmute microphone
     */
    fun setMuted(muted: Boolean) {
        setEnabled(!muted)
        Log.d(TAG, "Microphone muted: $muted")
    }
    
    /**
     * Get current audio track
     */
    fun getAudioTrack(): AudioTrack? = audioTrack
    
    /**
     * Check if microphone is capturing
     */
    fun isCapturing(): Boolean = isCapturing
    
    /**
     * Set audio volume (0.0 to 1.0)
     */
    fun setVolume(volume: Double) {
        audioTrack?.setVolume(volume)
        Log.d(TAG, "Audio volume set to: $volume")
    }
    
    /**
     * Release all resources
     */
    fun release() {
        Log.d(TAG, "Releasing microphone resources")
        
        audioTrack?.setEnabled(false)
        audioTrack?.dispose()
        audioTrack = null
        
        audioSource?.dispose()
        audioSource = null
        
        audioConstraints = null
        isCapturing = false
        
        Log.d(TAG, "Microphone resources released")
    }
}

/**
 * Audio processing options
 */
data class AudioProcessingOptions(
    val echoCancellation: Boolean = true,
    val noiseSuppression: Boolean = true,
    val autoGainControl: Boolean = true,
    val highPassFilter: Boolean = true,
    val typingNoiseDetection: Boolean = true
)
