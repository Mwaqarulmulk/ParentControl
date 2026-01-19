package com.myparentalcontrol.parent.ui.location

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.myparentalcontrol.parent.data.repository.ChildDeviceRepository
import com.myparentalcontrol.parent.data.repository.CommandRepository
import com.myparentalcontrol.shared.ParentalControlApp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * ViewModel for Live Location Screen with real-time updates
 */
@HiltViewModel
class LiveLocationViewModel @Inject constructor(
    private val database: FirebaseDatabase,
    private val firestore: FirebaseFirestore,
    private val childDeviceRepository: ChildDeviceRepository,
    private val commandRepository: CommandRepository
) : ViewModel() {
    
    companion object {
        private const val TAG = "LiveLocationViewModel"
    }
    
    private val _uiState = MutableStateFlow(LiveLocationUiState())
    val uiState: StateFlow<LiveLocationUiState> = _uiState.asStateFlow()
    
    private val _geofences = MutableStateFlow<List<GeofenceItem>>(emptyList())
    val geofences: StateFlow<List<GeofenceItem>> = _geofences.asStateFlow()
    
    private val _locationHistory = MutableStateFlow<List<LocationHistoryItem>>(emptyList())
    val locationHistory: StateFlow<List<LocationHistoryItem>> = _locationHistory.asStateFlow()
    
    private var deviceId: String = ""
    private var locationListener: ValueEventListener? = null
    private var deviceListener: ValueEventListener? = null
    
    /**
     * Initialize with device ID and start real-time listening
     */
    fun initialize(deviceId: String) {
        this.deviceId = deviceId
        Log.d(TAG, "Initializing live location for device: $deviceId")
        
        _uiState.update { it.copy(isLoading = true) }
        
        // Start real-time location listening
        startRealtimeLocationUpdates()
        
        // Load device status
        loadDeviceStatus()
        
        // Load geofences
        loadGeofences()
        
        // Load location history
        loadLocationHistory()
    }
    
    /**
     * Start listening for real-time location updates from Firebase Realtime Database
     */
    private fun startRealtimeLocationUpdates() {
        val locationRef = database.getReference("${ParentalControlApp.RealtimePaths.LOCATIONS}/$deviceId/current")
        
        locationListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val latitude = snapshot.child("latitude").getValue(Double::class.java) ?: 0.0
                    val longitude = snapshot.child("longitude").getValue(Double::class.java) ?: 0.0
                    val accuracy = snapshot.child("accuracy").getValue(Float::class.java) ?: 0f
                    val timestamp = snapshot.child("timestamp").getValue(Long::class.java) ?: 0L
                    
                    Log.d(TAG, "Real-time location update: $latitude, $longitude")
                    
                    val formattedTime = if (timestamp > 0) {
                        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
                    } else null
                    
                    _uiState.update { 
                        it.copy(
                            currentLocation = LatLng(latitude, longitude),
                            accuracy = accuracy,
                            lastUpdateTime = formattedTime,
                            isLoading = false
                        )
                    }
                    
                    // Check geofences
                    checkGeofences(latitude, longitude)
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Location listener cancelled: ${error.message}")
                _uiState.update { it.copy(error = error.message, isLoading = false) }
            }
        }
        
        locationRef.addValueEventListener(locationListener!!)
    }
    
    /**
     * Load device status from Firestore
     */
    private fun loadDeviceStatus() {
        viewModelScope.launch {
            childDeviceRepository.getChildDevice(deviceId).collect { device ->
                if (device != null) {
                    _uiState.update { 
                        it.copy(
                            batteryLevel = device.batteryLevel,
                            isOnline = device.isOnline,
                            currentLocation = if (device.latitude != 0.0 && device.longitude != 0.0) {
                                LatLng(device.latitude, device.longitude)
                            } else it.currentLocation
                        )
                    }
                }
            }
        }
    }
    
    /**
     * Load geofences for this device
     */
    private fun loadGeofences() {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("geofences")
                    .whereEqualTo("deviceId", deviceId)
                    .get()
                    .await()
                
                val geofenceList = snapshot.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    GeofenceItem(
                        id = doc.id,
                        name = data["name"] as? String ?: "",
                        latitude = data["latitude"] as? Double ?: 0.0,
                        longitude = data["longitude"] as? Double ?: 0.0,
                        radius = (data["radius"] as? Number)?.toFloat() ?: 100f,
                        isInside = false
                    )
                }
                
                _geofences.value = geofenceList
                Log.d(TAG, "Loaded ${geofenceList.size} geofences")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading geofences", e)
            }
        }
    }
    
    /**
     * Load location history from Firestore
     */
    private fun loadLocationHistory() {
        viewModelScope.launch {
            val result = childDeviceRepository.getLocationHistory(deviceId, 50)
            result.onSuccess { history ->
                _locationHistory.value = history.map { loc ->
                    LocationHistoryItem(
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        timestamp = loc.timestamp,
                        accuracy = loc.accuracy
                    )
                }
                Log.d(TAG, "Loaded ${history.size} location history entries")
            }
        }
    }
    
    /**
     * Request immediate location update from child device
     */
    fun requestLocationUpdate() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val result = commandRepository.requestLocationUpdate(deviceId)
            result.onSuccess {
                Log.d(TAG, "Location update requested")
            }.onFailure { error ->
                Log.e(TAG, "Failed to request location update", error)
                _uiState.update { it.copy(error = error.message) }
            }
            
            _uiState.update { it.copy(isLoading = false) }
        }
    }
    
    /**
     * Add a new geofence
     */
    fun addGeofence(name: String, latitude: Double, longitude: Double, radius: Float) {
        viewModelScope.launch {
            try {
                val geofenceData = hashMapOf(
                    "deviceId" to deviceId,
                    "name" to name,
                    "latitude" to latitude,
                    "longitude" to longitude,
                    "radius" to radius,
                    "createdAt" to System.currentTimeMillis(),
                    "notifyOnEnter" to true,
                    "notifyOnExit" to true
                )
                
                val docRef = firestore.collection("geofences")
                    .add(geofenceData)
                    .await()
                
                // Add to local list
                val newGeofence = GeofenceItem(
                    id = docRef.id,
                    name = name,
                    latitude = latitude,
                    longitude = longitude,
                    radius = radius,
                    isInside = false
                )
                
                _geofences.update { it + newGeofence }
                
                // Also send command to child device to register geofence
                sendGeofenceToChild(newGeofence)
                
                Log.d(TAG, "Geofence added: $name")
            } catch (e: Exception) {
                Log.e(TAG, "Error adding geofence", e)
                _uiState.update { it.copy(error = "Failed to add geofence: ${e.message}") }
            }
        }
    }
    
    /**
     * Remove a geofence
     */
    fun removeGeofence(geofenceId: String) {
        viewModelScope.launch {
            try {
                firestore.collection("geofences")
                    .document(geofenceId)
                    .delete()
                    .await()
                
                _geofences.update { list -> list.filter { it.id != geofenceId } }
                Log.d(TAG, "Geofence removed: $geofenceId")
            } catch (e: Exception) {
                Log.e(TAG, "Error removing geofence", e)
            }
        }
    }
    
    /**
     * Check if current location is inside any geofence
     */
    private fun checkGeofences(latitude: Double, longitude: Double) {
        _geofences.update { geofences ->
            geofences.map { geofence ->
                val distance = calculateDistance(
                    latitude, longitude,
                    geofence.latitude, geofence.longitude
                )
                geofence.copy(isInside = distance <= geofence.radius)
            }
        }
    }
    
    /**
     * Calculate distance between two points in meters
     */
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }
    
    /**
     * Send geofence to child device via Realtime Database
     */
    private fun sendGeofenceToChild(geofence: GeofenceItem) {
        viewModelScope.launch {
            try {
                database.getReference("${ParentalControlApp.RealtimePaths.COMMANDS}/$deviceId")
                    .push()
                    .setValue(
                        mapOf(
                            "type" to "ADD_GEOFENCE",
                            "geofenceId" to geofence.id,
                            "name" to geofence.name,
                            "latitude" to geofence.latitude,
                            "longitude" to geofence.longitude,
                            "radius" to geofence.radius,
                            "status" to "pending",
                            "timestamp" to System.currentTimeMillis()
                        )
                    )
                    .await()
            } catch (e: Exception) {
                Log.e(TAG, "Error sending geofence to child", e)
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        // Remove listeners
        locationListener?.let { listener ->
            database.getReference("${ParentalControlApp.RealtimePaths.LOCATIONS}/$deviceId/current")
                .removeEventListener(listener)
        }
    }
}
