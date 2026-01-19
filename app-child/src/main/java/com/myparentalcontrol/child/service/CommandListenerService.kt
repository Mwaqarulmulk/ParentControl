package com.myparentalcontrol.child.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.database.*
import com.myparentalcontrol.child.MainActivity
import com.myparentalcontrol.child.R
import com.myparentalcontrol.child.util.DeviceUtils
import com.myparentalcontrol.shared.ParentalControlApp
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

/**
 * Service that listens for commands from the parent app via Firebase Realtime Database
 * and executes them on the child device.
 */
@AndroidEntryPoint
class CommandListenerService : Service() {

    companion object {
        private const val TAG = "CommandListenerService"
        private const val NOTIFICATION_ID = 1009
        private const val CHANNEL_ID = "command_listener_channel"
        private const val CHANNEL_NAME = "Command Listener"

        const val ACTION_START = "com.myparentalcontrol.child.action.START_COMMAND_LISTENER"
        const val ACTION_STOP = "com.myparentalcontrol.child.action.STOP_COMMAND_LISTENER"

        fun startService(context: Context) {
            val intent = Intent(context, CommandListenerService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, CommandListenerService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    @Inject
    lateinit var database: FirebaseDatabase

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var commandListener: ChildEventListener? = null
    private var commandRef: DatabaseReference? = null
    private var deviceId: String = ""
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        createNotificationChannel()
        deviceId = DeviceUtils.getDeviceId(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForeground(NOTIFICATION_ID, createNotification())
                startListeningForCommands()
                Log.d(TAG, "Command listener started for device: $deviceId")
            }
            ACTION_STOP -> {
                Log.d(TAG, "Stopping command listener")
                stopListeningForCommands()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopListeningForCommands()
        mediaPlayer?.release()
        scope.cancel()
        Log.d(TAG, "Service destroyed")
    }

    private fun startListeningForCommands() {
        if (deviceId.isEmpty()) {
            Log.e(TAG, "Device ID is empty, cannot listen for commands")
            return
        }

        val path = "${ParentalControlApp.RealtimePaths.COMMANDS}/$deviceId"
        commandRef = database.getReference(path)
        
        commandListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                Log.d(TAG, "New command received: ${snapshot.key}")
                processCommand(snapshot)
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                // Commands are typically not updated, but handle if needed
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                // Command removed, no action needed
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                // Commands don't move
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Command listener cancelled: ${error.message}")
            }
        }

        commandRef?.addChildEventListener(commandListener!!)
        Log.d(TAG, "Started listening for commands at: $path")
    }

    private fun stopListeningForCommands() {
        commandListener?.let {
            commandRef?.removeEventListener(it)
        }
        commandListener = null
        commandRef = null
    }

    private fun processCommand(snapshot: DataSnapshot) {
        val commandId = snapshot.key ?: return
        val status = snapshot.child("status").getValue(String::class.java) ?: "pending"
        
        // Only process pending commands
        if (status != "pending") {
            Log.d(TAG, "Skipping command $commandId with status: $status")
            return
        }

        val commandType = snapshot.child("type").getValue(String::class.java) ?: return
        Log.d(TAG, "Processing command: $commandType (ID: $commandId)")

        // Mark as processing
        updateCommandStatus(commandId, "processing")

        scope.launch {
            try {
                when (commandType) {
                    ParentalControlApp.CommandTypes.START_CAMERA_STREAM -> {
                        val cameraType = snapshot.child("cameraType").getValue(String::class.java) ?: "front"
                        val withAudio = snapshot.child("withAudio").getValue(Boolean::class.java) ?: false
                        startCameraStream(cameraType, withAudio)
                    }
                    ParentalControlApp.CommandTypes.STOP_CAMERA_STREAM -> {
                        stopCameraStream()
                    }
                    ParentalControlApp.CommandTypes.START_SCREEN_MIRROR -> {
                        val withAudio = snapshot.child("withAudio").getValue(Boolean::class.java) ?: false
                        startScreenMirror(withAudio)
                    }
                    ParentalControlApp.CommandTypes.STOP_SCREEN_MIRROR -> {
                        stopScreenMirror()
                    }
                    ParentalControlApp.CommandTypes.START_AUDIO_STREAM -> {
                        startAudioStream()
                    }
                    ParentalControlApp.CommandTypes.STOP_AUDIO_STREAM -> {
                        stopAudioStream()
                    }
                    ParentalControlApp.CommandTypes.UPDATE_LOCATION -> {
                        requestLocationUpdate()
                    }
                    ParentalControlApp.CommandTypes.PLAY_SOUND -> {
                        playRingtone()
                    }
                    ParentalControlApp.CommandTypes.SYNC_DATA -> {
                        syncData()
                    }
                    // Snapshot commands
                    "TAKE_CAMERA_SNAPSHOT" -> {
                        takeCameraSnapshot()
                    }
                    "TAKE_SCREEN_SNAPSHOT" -> {
                        takeScreenSnapshot()
                    }
                    // Recording commands
                    "START_CAMERA_RECORDING" -> {
                        val duration = snapshot.child("duration").getValue(Int::class.java) ?: 60
                        startCameraRecording(duration)
                    }
                    "STOP_CAMERA_RECORDING" -> {
                        stopCameraRecording()
                    }
                    "START_SCREEN_RECORDING" -> {
                        val duration = snapshot.child("duration").getValue(Int::class.java) ?: 60
                        startScreenRecording(duration)
                    }
                    "STOP_SCREEN_RECORDING" -> {
                        stopScreenRecording()
                    }
                    "START_AMBIENT_RECORDING" -> {
                        val duration = snapshot.child("duration").getValue(Int::class.java) ?: 60
                        startAmbientRecording(duration)
                    }
                    "STOP_AMBIENT_RECORDING" -> {
                        stopAmbientRecording()
                    }
                    else -> {
                        Log.w(TAG, "Unknown command type: $commandType")
                        updateCommandStatus(commandId, "failed", "Unknown command type")
                        return@launch
                    }
                }
                
                // Mark as completed
                updateCommandStatus(commandId, "completed")
                Log.d(TAG, "Command $commandId completed successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error executing command $commandId", e)
                updateCommandStatus(commandId, "failed", e.message ?: "Unknown error")
            }
        }
    }

    private fun updateCommandStatus(commandId: String, status: String, error: String? = null) {
        val updates = mutableMapOf<String, Any>(
            "status" to status,
            "processedAt" to System.currentTimeMillis()
        )
        error?.let { updates["error"] = it }

        commandRef?.child(commandId)?.updateChildren(updates)
    }

    // Command implementations
    private fun startCameraStream(cameraType: String, withAudio: Boolean) {
        Log.d(TAG, "Starting camera stream: camera=$cameraType, audio=$withAudio")
        CameraStreamingService.startService(this, cameraType, withAudio)
    }

    private fun stopCameraStream() {
        Log.d(TAG, "Stopping camera stream")
        CameraStreamingService.stopService(this)
    }

    private fun startScreenMirror(withAudio: Boolean) {
        Log.d(TAG, "Starting screen mirror: audio=$withAudio")
        ScreenMirroringService.startService(this, withAudio)
    }

    private fun stopScreenMirror() {
        Log.d(TAG, "Stopping screen mirror")
        ScreenMirroringService.stopService(this)
    }

    private fun startAudioStream() {
        Log.d(TAG, "Starting audio stream")
        AudioStreamingService.startService(this)
    }

    private fun stopAudioStream() {
        Log.d(TAG, "Stopping audio stream")
        AudioStreamingService.stopService(this)
    }

    private fun requestLocationUpdate() {
        Log.d(TAG, "Requesting immediate location update")
        LocationTrackingService.requestImmediateUpdate(this)
    }

    private suspend fun playRingtone() {
        Log.d(TAG, "Playing ringtone to locate device")
        withContext(Dispatchers.Main) {
            try {
                // Vibrate
                vibrateDevice()
                
                // Play ringtone
                val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                mediaPlayer?.release()
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .build()
                    )
                    setDataSource(this@CommandListenerService, ringtoneUri)
                    isLooping = true
                    prepare()
                    start()
                }
                
                // Stop after 30 seconds
                delay(30000)
                mediaPlayer?.stop()
                mediaPlayer?.release()
                mediaPlayer = null
                
            } catch (e: Exception) {
                Log.e(TAG, "Error playing ringtone", e)
            }
        }
    }

    private fun vibrateDevice() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createWaveform(
                    longArrayOf(0, 500, 200, 500, 200, 500),
                    -1
                )
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 500, 200, 500, 200, 500), -1)
        }
    }

    private fun syncData() {
        Log.d(TAG, "Syncing all data")
        // Trigger updates from all services
        LocationTrackingService.requestImmediateUpdate(this)
        // Other sync actions can be added here
    }
    
    // Snapshot implementations
    private fun takeCameraSnapshot() {
        Log.d(TAG, "Taking camera snapshot")
        SnapshotService.takeCameraSnapshot(this)
    }
    
    private fun takeScreenSnapshot() {
        Log.d(TAG, "Taking screen snapshot")
        SnapshotService.takeScreenSnapshot(this)
    }
    
    // Recording implementations
    private fun startCameraRecording(durationSeconds: Int) {
        Log.d(TAG, "Starting camera recording for $durationSeconds seconds")
        RecordingService.startCameraRecording(this, durationSeconds)
    }
    
    private fun stopCameraRecording() {
        Log.d(TAG, "Stopping camera recording")
        RecordingService.stopCameraRecording(this)
    }
    
    private fun startScreenRecording(durationSeconds: Int) {
        Log.d(TAG, "Starting screen recording for $durationSeconds seconds")
        RecordingService.startScreenRecording(this, durationSeconds)
    }
    
    private fun stopScreenRecording() {
        Log.d(TAG, "Stopping screen recording")
        RecordingService.stopScreenRecording(this)
    }
    
    private fun startAmbientRecording(durationSeconds: Int) {
        Log.d(TAG, "Starting ambient recording for $durationSeconds seconds")
        RecordingService.startAmbientRecording(this, durationSeconds)
    }
    
    private fun stopAmbientRecording() {
        Log.d(TAG, "Stopping ambient recording")
        RecordingService.stopAmbientRecording(this)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Command listener service"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Command Listener Active")
            .setContentText("Listening for parent commands")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }
}
