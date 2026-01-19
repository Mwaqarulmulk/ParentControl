package com.myparentalcontrol.parent.data.repository

import com.google.firebase.database.FirebaseDatabase
import com.myparentalcontrol.shared.ParentalControlApp
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommandRepository @Inject constructor(
    private val database: FirebaseDatabase
) {
    
    /**
     * Send command to child device
     */
    private suspend fun sendCommand(deviceId: String, commandType: String, params: Map<String, Any> = emptyMap()): Result<Unit> {
        return try {
            val commandId = database.reference.push().key ?: return Result.failure(Exception("Failed to generate command ID"))
            
            val command = mutableMapOf<String, Any>(
                "type" to commandType,
                "status" to "pending",
                "timestamp" to System.currentTimeMillis()
            )
            command.putAll(params)
            
            database.getReference("${ParentalControlApp.RealtimePaths.COMMANDS}/$deviceId/$commandId")
                .setValue(command)
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Start camera stream
     */
    suspend fun startCameraStream(deviceId: String, cameraType: String = "front", withAudio: Boolean = false): Result<Unit> {
        return sendCommand(
            deviceId,
            ParentalControlApp.CommandTypes.START_CAMERA_STREAM,
            mapOf(
                "cameraType" to cameraType,
                "withAudio" to withAudio
            )
        )
    }
    
    /**
     * Stop camera stream
     */
    suspend fun stopCameraStream(deviceId: String): Result<Unit> {
        return sendCommand(deviceId, ParentalControlApp.CommandTypes.STOP_CAMERA_STREAM)
    }
    
    /**
     * Start screen mirror
     */
    suspend fun startScreenMirror(deviceId: String, withAudio: Boolean = false): Result<Unit> {
        return sendCommand(
            deviceId,
            ParentalControlApp.CommandTypes.START_SCREEN_MIRROR,
            mapOf("withAudio" to withAudio)
        )
    }
    
    /**
     * Stop screen mirror
     */
    suspend fun stopScreenMirror(deviceId: String): Result<Unit> {
        return sendCommand(deviceId, ParentalControlApp.CommandTypes.STOP_SCREEN_MIRROR)
    }
    
    /**
     * Start audio stream
     */
    suspend fun startAudioStream(deviceId: String): Result<Unit> {
        return sendCommand(deviceId, ParentalControlApp.CommandTypes.START_AUDIO_STREAM)
    }
    
    /**
     * Stop audio stream
     */
    suspend fun stopAudioStream(deviceId: String): Result<Unit> {
        return sendCommand(deviceId, ParentalControlApp.CommandTypes.STOP_AUDIO_STREAM)
    }
    
    /**
     * Request immediate location update
     */
    suspend fun requestLocationUpdate(deviceId: String): Result<Unit> {
        return sendCommand(deviceId, ParentalControlApp.CommandTypes.UPDATE_LOCATION)
    }
    
    /**
     * Ring device (play sound)
     */
    suspend fun ringDevice(deviceId: String): Result<Unit> {
        return sendCommand(deviceId, ParentalControlApp.CommandTypes.PLAY_SOUND)
    }
    
    /**
     * Force data sync
     */
    suspend fun syncData(deviceId: String): Result<Unit> {
        return sendCommand(deviceId, ParentalControlApp.CommandTypes.SYNC_DATA)
    }
    
    /**
     * Take camera snapshot
     */
    suspend fun takeCameraSnapshot(deviceId: String): Result<Unit> {
        return sendCommand(deviceId, "TAKE_CAMERA_SNAPSHOT")
    }
    
    /**
     * Take screen snapshot
     */
    suspend fun takeScreenSnapshot(deviceId: String): Result<Unit> {
        return sendCommand(deviceId, "TAKE_SCREEN_SNAPSHOT")
    }
    
    /**
     * Start camera recording
     */
    suspend fun startCameraRecording(deviceId: String, durationSeconds: Int = 60): Result<Unit> {
        return sendCommand(
            deviceId,
            "START_CAMERA_RECORDING",
            mapOf("duration" to durationSeconds)
        )
    }
    
    /**
     * Stop camera recording
     */
    suspend fun stopCameraRecording(deviceId: String): Result<Unit> {
        return sendCommand(deviceId, "STOP_CAMERA_RECORDING")
    }
    
    /**
     * Start screen recording
     */
    suspend fun startScreenRecording(deviceId: String, durationSeconds: Int = 60): Result<Unit> {
        return sendCommand(
            deviceId,
            "START_SCREEN_RECORDING",
            mapOf("duration" to durationSeconds)
        )
    }
    
    /**
     * Stop screen recording
     */
    suspend fun stopScreenRecording(deviceId: String): Result<Unit> {
        return sendCommand(deviceId, "STOP_SCREEN_RECORDING")
    }
    
    /**
     * Start ambient recording (audio only)
     */
    suspend fun startAmbientRecording(deviceId: String, durationSeconds: Int = 60): Result<Unit> {
        return sendCommand(
            deviceId,
            "START_AMBIENT_RECORDING",
            mapOf("duration" to durationSeconds)
        )
    }
    
    /**
     * Stop ambient recording
     */
    suspend fun stopAmbientRecording(deviceId: String): Result<Unit> {
        return sendCommand(deviceId, "STOP_AMBIENT_RECORDING")
    }
}
