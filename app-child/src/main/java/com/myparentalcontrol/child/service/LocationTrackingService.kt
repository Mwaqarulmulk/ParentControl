package com.myparentalcontrol.child.service

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.myparentalcontrol.child.MainActivity
import com.myparentalcontrol.child.R
import com.myparentalcontrol.child.util.DateTimeUtils
import com.myparentalcontrol.child.util.DeviceUtils
import com.myparentalcontrol.shared.ParentalControlApp
import com.myparentalcontrol.shared.data.supabase.SupabaseRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * FEATURE 5: LOCATION TRACKING
 * 
 * Tracks device GPS location and syncs to Firebase
 * - Configurable update interval (default: 15 minutes)
 * - Background location tracking
 * - Location history storage
 * - On-demand location updates
 */
@AndroidEntryPoint
class LocationTrackingService : Service() {

    companion object {
        private const val TAG = "LocationTrackingService"
        private const val NOTIFICATION_ID = 1005
        private const val LOCATION_UPDATE_INTERVAL = 15 * 60 * 1000L // 15 minutes
        private const val FASTEST_LOCATION_INTERVAL = 5 * 60 * 1000L // 5 minutes

        const val ACTION_START = "com.myparentalcontrol.child.action.START_LOCATION_TRACKING"
        const val ACTION_STOP = "com.myparentalcontrol.child.action.STOP_LOCATION_TRACKING"
        const val ACTION_UPDATE_NOW = "com.myparentalcontrol.child.action.UPDATE_LOCATION_NOW"

        fun startService(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
        
        fun requestImmediateUpdate(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = ACTION_UPDATE_NOW
            }
            context.startService(intent)
        }
    }

    @Inject
    lateinit var firestore: FirebaseFirestore
    
    @Inject
    lateinit var database: FirebaseDatabase
    
    @Inject
    lateinit var supabaseRepository: SupabaseRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var deviceId: String = ""
    private var isTracking = false

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { location ->
                Log.d(TAG, "Location updated: ${location.latitude}, ${location.longitude}")
                saveLocationToFirebase(location)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        deviceId = DeviceUtils.getDeviceId(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForeground(NOTIFICATION_ID, createNotification())
                startLocationTracking()
            }
            ACTION_STOP -> {
                stopLocationTracking()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_UPDATE_NOW -> {
                requestSingleLocationUpdate()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startLocationTracking() {
        if (isTracking) return
        
        if (!hasLocationPermissions()) {
            Log.e(TAG, "Location permissions not granted")
            stopSelf()
            return
        }

        try {
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                LOCATION_UPDATE_INTERVAL
            ).apply {
                setMinUpdateIntervalMillis(FASTEST_LOCATION_INTERVAL)
                setWaitForAccurateLocation(false)
            }.build()

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            
            isTracking = true
            Log.d(TAG, "Location tracking started")
            
            // Get initial location
            requestSingleLocationUpdate()
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException while requesting location updates", e)
        }
    }

    private fun stopLocationTracking() {
        if (!isTracking) return
        
        fusedLocationClient.removeLocationUpdates(locationCallback)
        isTracking = false
        Log.d(TAG, "Location tracking stopped")
    }

    private fun requestSingleLocationUpdate() {
        if (!hasLocationPermissions()) return

        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    Log.d(TAG, "Got last known location: ${it.latitude}, ${it.longitude}")
                    saveLocationToFirebase(it)
                } ?: run {
                    Log.d(TAG, "No last known location available")
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException while getting last location", e)
        }
    }

    private fun saveLocationToFirebase(location: Location) {
        scope.launch {
            try {
                val timestamp = DateTimeUtils.getCurrentTimestamp()
                
                val locationData = hashMapOf(
                    "deviceId" to deviceId,
                    "latitude" to location.latitude,
                    "longitude" to location.longitude,
                    "accuracy" to location.accuracy,
                    "altitude" to location.altitude,
                    "speed" to location.speed,
                    "bearing" to location.bearing,
                    "timestamp" to timestamp,
                    "provider" to (location.provider ?: "unknown")
                )

                // Save to Firebase Realtime Database for real-time parent updates
                val realtimeLocationData = mapOf(
                    "latitude" to location.latitude,
                    "longitude" to location.longitude,
                    "accuracy" to location.accuracy,
                    "timestamp" to timestamp
                )
                
                database.getReference("${ParentalControlApp.RealtimePaths.LOCATIONS}/$deviceId/current")
                    .setValue(realtimeLocationData)
                    .await()
                
                Log.d(TAG, "Location saved to Realtime Database")

                // Save to location history in Firestore
                firestore.collection(ParentalControlApp.FirebaseCollections.LOCATIONS)
                    .document(deviceId)
                    .collection("history")
                    .add(locationData)
                    .await()

                // Update current location in device document
                firestore.collection(ParentalControlApp.FirebaseCollections.DEVICES)
                    .document(deviceId)
                    .update(
                        mapOf(
                            "latitude" to location.latitude,
                            "longitude" to location.longitude,
                            "locationAccuracy" to location.accuracy,
                            "locationUpdatedAt" to DateTimeUtils.getCurrentTimestamp()
                        )
                    )
                    .await()
                
                // 4. Also save to Supabase for cross-platform support
                supabaseRepository.insertLocation(
                    deviceId = deviceId,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    accuracy = location.accuracy,
                    altitude = location.altitude,
                    speed = location.speed,
                    provider = location.provider
                )

                Log.d(TAG, "Location saved to Firebase + Supabase")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save location to Firebase", e)
            }
        }
    }

    private fun hasLocationPermissions(): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        val coarseLocation = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        return fineLocation || coarseLocation
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, ParentalControlApp.NotificationChannels.MONITORING)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Location Tracking Active")
            .setContentText("Tracking your location")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationTracking()
        Log.d(TAG, "Service destroyed")
    }
}
