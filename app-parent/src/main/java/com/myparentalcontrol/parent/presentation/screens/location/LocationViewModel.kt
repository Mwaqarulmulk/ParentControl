package com.myparentalcontrol.parent.presentation.screens.location

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class LocationData(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val accuracy: Float = 0f,
    val timestamp: Long = 0L,
    val timeAgo: String = "",
    val address: String = "",
    val provider: String = ""
)

data class LocationUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val currentLocation: LocationData? = null,
    val locationHistory: List<LocationData> = emptyList(),
    val showHistory: Boolean = false,
    val isSatelliteView: Boolean = false,
    val isLive: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class LocationViewModel @Inject constructor(
    private val database: FirebaseDatabase,
    private val firestore: FirebaseFirestore
) : ViewModel() {
    
    companion object {
        private const val TAG = "LocationViewModel"
    }
    
    private val _uiState = MutableStateFlow(LocationUiState())
    val uiState: StateFlow<LocationUiState> = _uiState.asStateFlow()
    
    private var deviceId: String = ""
    private var locationListener: ValueEventListener? = null
    
    fun startListening(childDeviceId: String) {
        deviceId = childDeviceId
        _uiState.update { it.copy(isLoading = true) }
        
        listenToCurrentLocation()
        loadLocationHistory()
    }
    
    private fun listenToCurrentLocation() {
        val ref = database.getReference("locations/$deviceId/current")
        
        locationListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val location = parseLocation(snapshot)
                    location?.let { loc ->
                        // Check if location is recent (within last 5 minutes)
                        val isLive = (System.currentTimeMillis() - loc.timestamp) < 300_000
                        
                        _uiState.update { state ->
                            state.copy(
                                currentLocation = loc,
                                isLive = isLive,
                                isLoading = false,
                                error = null
                            )
                        }
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Location listener cancelled: ${error.message}")
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        error = error.message
                    ) 
                }
            }
        }
        
        ref.addValueEventListener(locationListener!!)
    }
    
    private fun parseLocation(snapshot: DataSnapshot): LocationData? {
        return try {
            val latitude = snapshot.child("latitude").getValue(Double::class.java) ?: 0.0
            val longitude = snapshot.child("longitude").getValue(Double::class.java) ?: 0.0
            val accuracy = snapshot.child("accuracy").getValue(Float::class.java) ?: 0f
            val timestamp = snapshot.child("timestamp").getValue(Long::class.java) ?: 0L
            val address = snapshot.child("address").getValue(String::class.java) ?: ""
            val provider = snapshot.child("provider").getValue(String::class.java) ?: ""
            
            LocationData(
                latitude = latitude,
                longitude = longitude,
                accuracy = accuracy,
                timestamp = timestamp,
                timeAgo = formatTimeAgo(timestamp),
                address = address,
                provider = provider
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing location", e)
            null
        }
    }
    
    private fun loadLocationHistory() {
        viewModelScope.launch {
            try {
                // Load from Firestore location history
                val snapshot = firestore
                    .collection("devices")
                    .document(deviceId)
                    .collection("locationHistory")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(50)
                    .get()
                    .await()
                
                val history = snapshot.documents.mapNotNull { doc ->
                    try {
                        LocationData(
                            latitude = doc.getDouble("latitude") ?: 0.0,
                            longitude = doc.getDouble("longitude") ?: 0.0,
                            accuracy = (doc.getDouble("accuracy") ?: 0.0).toFloat(),
                            timestamp = doc.getLong("timestamp") ?: 0L,
                            timeAgo = formatTimeAgo(doc.getLong("timestamp") ?: 0L),
                            address = doc.getString("address") ?: "",
                            provider = doc.getString("provider") ?: ""
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                
                _uiState.update { it.copy(locationHistory = history) }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading location history", e)
            }
        }
    }
    
    private fun formatTimeAgo(timestamp: Long): String {
        if (timestamp == 0L) return "Unknown"
        
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        return when {
            diff < 60_000 -> "Just now"
            diff < 3600_000 -> "${diff / 60_000}m ago"
            diff < 86400_000 -> "${diff / 3600_000}h ago"
            diff < 604800_000 -> "${diff / 86400_000}d ago"
            else -> {
                val sdf = java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault())
                sdf.format(java.util.Date(timestamp))
            }
        }
    }
    
    fun requestLocationUpdate(childDeviceId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            
            try {
                // Send command to child device to update location
                val commandRef = database.getReference("commands/$childDeviceId")
                val command = mapOf(
                    "type" to "UPDATE_LOCATION",
                    "timestamp" to ServerValue.TIMESTAMP,
                    "status" to "pending"
                )
                
                commandRef.push().setValue(command).await()
                
                // Wait a bit for response
                kotlinx.coroutines.delay(2000)
                
                _uiState.update { it.copy(isRefreshing = false) }
            } catch (e: Exception) {
                Log.e(TAG, "Error requesting location update", e)
                _uiState.update { 
                    it.copy(
                        isRefreshing = false,
                        error = "Failed to request location: ${e.message}"
                    ) 
                }
            }
        }
    }
    
    fun toggleHistoryMode() {
        _uiState.update { it.copy(showHistory = !it.showHistory) }
        
        // Reload history if enabling
        if (_uiState.value.showHistory && _uiState.value.locationHistory.isEmpty()) {
            loadLocationHistory()
        }
    }
    
    fun toggleSatelliteView() {
        _uiState.update { it.copy(isSatelliteView = !it.isSatelliteView) }
    }
    
    fun stopListening() {
        locationListener?.let {
            database.getReference("locations/$deviceId/current").removeEventListener(it)
        }
        locationListener = null
    }
    
    override fun onCleared() {
        super.onCleared()
        stopListening()
    }
}
