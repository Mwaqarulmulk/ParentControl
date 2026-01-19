package com.myparentalcontrol.child.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
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
import java.io.ByteArrayOutputStream
import java.util.*
import javax.inject.Inject

/**
 * Service for taking camera and screen snapshots
 */
@AndroidEntryPoint
class SnapshotService : Service() {

    companion object {
        private const val TAG = "SnapshotService"
        private const val NOTIFICATION_ID = 1010
        private const val CHANNEL_ID = "snapshot_channel"
        private const val CHANNEL_NAME = "Snapshot Service"

        const val ACTION_CAMERA_SNAPSHOT = "com.myparentalcontrol.child.action.CAMERA_SNAPSHOT"
        const val ACTION_SCREEN_SNAPSHOT = "com.myparentalcontrol.child.action.SCREEN_SNAPSHOT"
        
        fun takeCameraSnapshot(context: Context) {
            val intent = Intent(context, SnapshotService::class.java).apply {
                action = ACTION_CAMERA_SNAPSHOT
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun takeScreenSnapshot(context: Context) {
            val intent = Intent(context, SnapshotService::class.java).apply {
                action = ACTION_SCREEN_SNAPSHOT
            }
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
    private var handlerThread: HandlerThread? = null
    private var handler: Handler? = null
    private var cameraDevice: CameraDevice? = null
    private var imageReader: ImageReader? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        createNotificationChannel()
        
        handlerThread = HandlerThread("SnapshotThread").also { it.start() }
        handler = Handler(handlerThread!!.looper)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification("Taking snapshot..."))
        
        when (intent?.action) {
            ACTION_CAMERA_SNAPSHOT -> {
                scope.launch {
                    takeCameraSnapshotInternal()
                    stopSelf()
                }
            }
            ACTION_SCREEN_SNAPSHOT -> {
                scope.launch {
                    takeScreenSnapshotInternal()
                    stopSelf()
                }
            }
            else -> {
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        cameraDevice?.close()
        imageReader?.close()
        handlerThread?.quitSafely()
        scope.cancel()
        Log.d(TAG, "Service destroyed")
    }

    private suspend fun takeCameraSnapshotInternal() {
        try {
            Log.d(TAG, "Taking camera snapshot")
            
            val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
            } ?: cameraManager.cameraIdList.firstOrNull() ?: run {
                Log.e(TAG, "No camera available")
                return
            }
            
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            val streamConfigMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            val jpegSizes = streamConfigMap?.getOutputSizes(ImageFormat.JPEG)
            val size = jpegSizes?.maxByOrNull { it.width * it.height } ?: run {
                Log.e(TAG, "No JPEG sizes available")
                return
            }
            
            imageReader = ImageReader.newInstance(size.width, size.height, ImageFormat.JPEG, 1)
            
            val captureResult = CompletableDeferred<ByteArray?>()
            
            imageReader?.setOnImageAvailableListener({ reader ->
                val image = reader.acquireLatestImage()
                if (image != null) {
                    val buffer = image.planes[0].buffer
                    val bytes = ByteArray(buffer.remaining())
                    buffer.get(bytes)
                    image.close()
                    captureResult.complete(bytes)
                } else {
                    captureResult.complete(null)
                }
            }, handler)
            
            suspendCancellableCoroutine<Unit> { continuation ->
                try {
                    cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                        override fun onOpened(camera: CameraDevice) {
                            cameraDevice = camera
                            try {
                                val captureRequest = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
                                captureRequest.addTarget(imageReader!!.surface)
                                
                                camera.createCaptureSession(
                                    listOf(imageReader!!.surface),
                                    object : CameraCaptureSession.StateCallback() {
                                        override fun onConfigured(session: CameraCaptureSession) {
                                            try {
                                                session.capture(captureRequest.build(), null, handler)
                                                continuation.resume(Unit) {}
                                            } catch (e: Exception) {
                                                Log.e(TAG, "Capture failed", e)
                                                continuation.resume(Unit) {}
                                            }
                                        }
                                        
                                        override fun onConfigureFailed(session: CameraCaptureSession) {
                                            Log.e(TAG, "Session configuration failed")
                                            continuation.resume(Unit) {}
                                        }
                                    },
                                    handler
                                )
                            } catch (e: Exception) {
                                Log.e(TAG, "Error creating capture session", e)
                                continuation.resume(Unit) {}
                            }
                        }
                        
                        override fun onDisconnected(camera: CameraDevice) {
                            camera.close()
                            continuation.resume(Unit) {}
                        }
                        
                        override fun onError(camera: CameraDevice, error: Int) {
                            camera.close()
                            Log.e(TAG, "Camera error: $error")
                            continuation.resume(Unit) {}
                        }
                    }, handler)
                } catch (e: SecurityException) {
                    Log.e(TAG, "Camera permission denied", e)
                    continuation.resume(Unit) {}
                }
            }
            
            // Wait for image with timeout
            val imageBytes = withTimeoutOrNull(5000) { captureResult.await() }
            
            if (imageBytes != null) {
                uploadSnapshot(imageBytes, "camera")
            } else {
                Log.e(TAG, "Failed to capture camera image")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error taking camera snapshot", e)
        } finally {
            cameraDevice?.close()
            cameraDevice = null
        }
    }

    private suspend fun takeScreenSnapshotInternal() {
        // Screen snapshot requires MediaProjection which needs user interaction
        // For now, we'll save a placeholder and note that this requires MediaProjection setup
        Log.d(TAG, "Screen snapshot requested - requires MediaProjection permission")
        
        // Notify parent that screen snapshot requires permission
        val deviceId = DeviceUtils.getDeviceId(this)
        database.getReference("snapshots/$deviceId")
            .push()
            .setValue(mapOf(
                "type" to "screen",
                "status" to "requires_permission",
                "timestamp" to System.currentTimeMillis(),
                "message" to "Screen capture requires user permission grant"
            ))
    }

    private suspend fun uploadSnapshot(imageBytes: ByteArray, type: String) {
        try {
            val deviceId = DeviceUtils.getDeviceId(this)
            val timestamp = System.currentTimeMillis()
            
            // Compress image to reduce size for Base64 storage
            val compressedBytes = compressImage(imageBytes, 50) // 50% quality
            
            // Convert to Base64 for storage in Realtime Database
            val base64Image = android.util.Base64.encodeToString(compressedBytes, android.util.Base64.NO_WRAP)
            
            // Check size - RTDB has 10MB limit per write, keep images under 1MB
            val sizeKB = base64Image.length / 1024
            Log.d(TAG, "Snapshot size: ${sizeKB}KB")
            
            if (sizeKB > 900) {
                // Re-compress with lower quality if too large
                val smallerBytes = compressImage(imageBytes, 25)
                val smallerBase64 = android.util.Base64.encodeToString(smallerBytes, android.util.Base64.NO_WRAP)
                
                database.getReference("snapshots/$deviceId")
                    .push()
                    .setValue(mapOf(
                        "type" to type,
                        "imageData" to smallerBase64,
                        "timestamp" to timestamp,
                        "status" to "completed",
                        "sizeKB" to (smallerBase64.length / 1024)
                    ))
            } else {
                database.getReference("snapshots/$deviceId")
                    .push()
                    .setValue(mapOf(
                        "type" to type,
                        "imageData" to base64Image,
                        "timestamp" to timestamp,
                        "status" to "completed",
                        "sizeKB" to sizeKB
                    ))
            }
            
            Log.d(TAG, "Snapshot uploaded to database successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading snapshot", e)
        }
    }
    
    private fun compressImage(imageBytes: ByteArray, quality: Int): ByteArray {
        return try {
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            bitmap.recycle()
            outputStream.toByteArray()
        } catch (e: Exception) {
            Log.e(TAG, "Error compressing image", e)
            imageBytes
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Snapshot service"
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
            .setContentTitle("Snapshot")
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
