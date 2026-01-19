package com.myparentalcontrol.child.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.myparentalcontrol.child.MainActivity
import com.myparentalcontrol.child.R
import com.myparentalcontrol.child.util.DeviceUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.io.File
import javax.inject.Inject

/**
 * Service for recording camera, screen, and ambient audio
 */
@AndroidEntryPoint
class RecordingService : Service() {

    companion object {
        private const val TAG = "RecordingService"
        private const val NOTIFICATION_ID = 1011
        private const val CHANNEL_ID = "recording_channel"
        private const val CHANNEL_NAME = "Recording Service"

        const val ACTION_START_CAMERA = "com.myparentalcontrol.child.action.START_CAMERA_RECORDING"
        const val ACTION_STOP_CAMERA = "com.myparentalcontrol.child.action.STOP_CAMERA_RECORDING"
        const val ACTION_START_SCREEN = "com.myparentalcontrol.child.action.START_SCREEN_RECORDING"
        const val ACTION_STOP_SCREEN = "com.myparentalcontrol.child.action.STOP_SCREEN_RECORDING"
        const val ACTION_START_AMBIENT = "com.myparentalcontrol.child.action.START_AMBIENT_RECORDING"
        const val ACTION_STOP_AMBIENT = "com.myparentalcontrol.child.action.STOP_AMBIENT_RECORDING"
        const val EXTRA_DURATION = "duration"

        fun startCameraRecording(context: Context, durationSeconds: Int) {
            val intent = Intent(context, RecordingService::class.java).apply {
                action = ACTION_START_CAMERA
                putExtra(EXTRA_DURATION, durationSeconds)
            }
            startServiceCompat(context, intent)
        }

        fun stopCameraRecording(context: Context) {
            val intent = Intent(context, RecordingService::class.java).apply {
                action = ACTION_STOP_CAMERA
            }
            context.startService(intent)
        }

        fun startScreenRecording(context: Context, durationSeconds: Int) {
            val intent = Intent(context, RecordingService::class.java).apply {
                action = ACTION_START_SCREEN
                putExtra(EXTRA_DURATION, durationSeconds)
            }
            startServiceCompat(context, intent)
        }

        fun stopScreenRecording(context: Context) {
            val intent = Intent(context, RecordingService::class.java).apply {
                action = ACTION_STOP_SCREEN
            }
            context.startService(intent)
        }

        fun startAmbientRecording(context: Context, durationSeconds: Int) {
            val intent = Intent(context, RecordingService::class.java).apply {
                action = ACTION_START_AMBIENT
                putExtra(EXTRA_DURATION, durationSeconds)
            }
            startServiceCompat(context, intent)
        }

        fun stopAmbientRecording(context: Context) {
            val intent = Intent(context, RecordingService::class.java).apply {
                action = ACTION_STOP_AMBIENT
            }
            context.startService(intent)
        }

        private fun startServiceCompat(context: Context, intent: Intent) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    @Inject
    lateinit var database: FirebaseDatabase

    @Inject
    lateinit var storage: FirebaseStorage

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var mediaRecorder: MediaRecorder? = null
    private var currentRecordingFile: File? = null
    private var currentRecordingType: String? = null
    private var recordingJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val duration = intent?.getIntExtra(EXTRA_DURATION, 60) ?: 60
        
        when (intent?.action) {
            ACTION_START_CAMERA -> {
                startForeground(NOTIFICATION_ID, createNotification("Recording camera..."))
                startCameraRecordingInternal(duration)
            }
            ACTION_STOP_CAMERA -> {
                stopCurrentRecording()
            }
            ACTION_START_SCREEN -> {
                startForeground(NOTIFICATION_ID, createNotification("Recording screen..."))
                // Screen recording requires MediaProjection - notify parent
                notifyScreenRecordingRequiresPermission()
                stopSelf()
            }
            ACTION_STOP_SCREEN -> {
                stopCurrentRecording()
            }
            ACTION_START_AMBIENT -> {
                startForeground(NOTIFICATION_ID, createNotification("Recording audio..."))
                startAmbientRecordingInternal(duration)
            }
            ACTION_STOP_AMBIENT -> {
                stopCurrentRecording()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopCurrentRecording()
        scope.cancel()
        Log.d(TAG, "Service destroyed")
    }

    private fun startCameraRecordingInternal(durationSeconds: Int) {
        // Camera recording with audio
        Log.d(TAG, "Starting camera recording for $durationSeconds seconds")
        // Note: Full camera recording requires Camera2 API with MediaRecorder
        // For simplicity, this implementation records ambient audio
        // Full video recording would require more complex Camera2 setup
        notifyCameraRecordingStarted()
        startAudioRecording("camera", durationSeconds)
    }

    private fun startAmbientRecordingInternal(durationSeconds: Int) {
        Log.d(TAG, "Starting ambient recording for $durationSeconds seconds")
        startAudioRecording("ambient", durationSeconds)
    }

    @Suppress("DEPRECATION")
    private fun startAudioRecording(type: String, durationSeconds: Int) {
        try {
            val outputFile = File(cacheDir, "${type}_${System.currentTimeMillis()}.m4a")
            currentRecordingFile = outputFile
            currentRecordingType = type

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(outputFile.absolutePath)
                setMaxDuration(durationSeconds * 1000)
                
                setOnInfoListener { _, what, _ ->
                    if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                        Log.d(TAG, "Max recording duration reached")
                        stopCurrentRecording()
                    }
                }
                
                prepare()
                start()
            }

            Log.d(TAG, "Recording started: ${outputFile.absolutePath}")

            // Auto-stop after duration
            recordingJob = scope.launch {
                delay(durationSeconds * 1000L)
                stopCurrentRecording()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error starting recording", e)
            stopSelf()
        }
    }

    private fun stopCurrentRecording() {
        recordingJob?.cancel()
        recordingJob = null

        try {
            mediaRecorder?.apply {
                stop()
                reset()
                release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recorder", e)
        }
        mediaRecorder = null

        // Upload recording
        currentRecordingFile?.let { file ->
            if (file.exists() && file.length() > 0) {
                scope.launch {
                    uploadRecording(file, currentRecordingType ?: "unknown")
                    file.delete()
                    withContext(Dispatchers.Main) {
                        stopSelf()
                    }
                }
            } else {
                stopSelf()
            }
        } ?: stopSelf()

        currentRecordingFile = null
        currentRecordingType = null
    }

    private suspend fun uploadRecording(file: File, type: String) {
        try {
            val deviceId = DeviceUtils.getDeviceId(this)
            val timestamp = System.currentTimeMillis()
            val filename = "${type}_${timestamp}.m4a"

            val storageRef = storage.reference
                .child("recordings")
                .child(deviceId)
                .child(filename)

            storageRef.putFile(android.net.Uri.fromFile(file)).await()
            val downloadUrl = storageRef.downloadUrl.await()

            // Save metadata to database
            database.getReference("recordings/$deviceId")
                .push()
                .setValue(mapOf(
                    "type" to type,
                    "url" to downloadUrl.toString(),
                    "timestamp" to timestamp,
                    "duration" to (file.length() / 1024), // Approximate size in KB
                    "status" to "completed"
                ))

            Log.d(TAG, "Recording uploaded successfully: $downloadUrl")

        } catch (e: Exception) {
            Log.e(TAG, "Error uploading recording", e)
        }
    }

    private fun notifyScreenRecordingRequiresPermission() {
        scope.launch {
            val deviceId = DeviceUtils.getDeviceId(this@RecordingService)
            database.getReference("recordings/$deviceId")
                .push()
                .setValue(mapOf(
                    "type" to "screen",
                    "status" to "requires_permission",
                    "timestamp" to System.currentTimeMillis(),
                    "message" to "Screen recording requires user permission grant"
                ))
        }
    }

    private fun notifyCameraRecordingStarted() {
        scope.launch {
            val deviceId = DeviceUtils.getDeviceId(this@RecordingService)
            database.getReference("recordings/$deviceId")
                .push()
                .setValue(mapOf(
                    "type" to "camera",
                    "status" to "recording",
                    "timestamp" to System.currentTimeMillis()
                ))
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Recording service"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(text: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Recording")
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T {
        return suspendCancellableCoroutine { continuation ->
            addOnSuccessListener { result ->
                continuation.resume(result) {}
            }
            addOnFailureListener { exception ->
                continuation.cancel(exception)
            }
        }
    }
}
