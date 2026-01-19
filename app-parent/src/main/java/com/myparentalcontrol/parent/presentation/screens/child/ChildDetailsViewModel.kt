package com.myparentalcontrol.parent.presentation.screens.child

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
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

data class ChildDetailsUiState(
    val isLoading: Boolean = true,
    val childDeviceId: String = "",
    val childName: String = "Child Device",
    val isOnline: Boolean = false,
    val batteryLevel: Int = 100,
    val lastSeen: String = "Unknown",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationUpdatedAt: String = "Never",
    val unreadNotifications: Int = 0,
    val error: String? = null
)

@HiltViewModel
class ChildDetailsViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val database: FirebaseDatabase
) : ViewModel() {
    
    companion object {
        private const val TAG = "ChildDetailsViewModel"
    }
    
    private val _uiState = MutableStateFlow(ChildDetailsUiState())
    val uiState: StateFlow<ChildDetailsUiState> = _uiState.asStateFlow()
    
    private var deviceStatusListener: ValueEventListener? = null
    private var locationListener: ValueEventListener? = null
    private var notificationCountListener: ValueEventListener? = null
    
    fun loadChildDetails(deviceId: String) {
        _uiState.update { it.copy(childDeviceId = deviceId, isLoading = true) }
        
        // Load from Firestore
        loadDeviceInfo(deviceId)
        
        // Listen to real-time updates
        listenToDeviceStatus(deviceId)
        listenToLocation(deviceId)
        listenToNotificationCount(deviceId)
    }
    
    private fun loadDeviceInfo(deviceId: String) {
        viewModelScope.launch {
            try {
                val doc = firestore.collection(ParentalControlApp.FirebaseCollections.DEVICES)
                    .document(deviceId)
                    .get()
                    .await()
                
                if (doc.exists()) {
                    val name = doc.getString("deviceName") ?: doc.getString("name") ?: "Child Device"
                    val battery = doc.getLong("batteryLevel")?.toInt() ?: 100
                    val lat = doc.getDouble("latitude")
                    val lng = doc.getDouble("longitude")
                    val locationTime = doc.getLong("locationUpdatedAt")
                    
                    _uiState.update {
                        it.copy(
                            childName = name,
                            batteryLevel = battery,
                            latitude = lat,
                            longitude = lng,
                            locationUpdatedAt = formatTimestamp(locationTime),
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading device info", e)
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
    
    private fun listenToDeviceStatus(deviceId: String) {
        val ref = database.getReference("${ParentalControlApp.RealtimePaths.DEVICES}/$deviceId/status")
        
        deviceStatusListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val isOnline = snapshot.child("isOnline").getValue(Boolean::class.java) ?: false
                val battery = snapshot.child("batteryLevel").getValue(Int::class.java) ?: 100
                val lastSeen = snapshot.child("lastSeen").getValue(Long::class.java)
                
                _uiState.update {
                    it.copy(
                        isOnline = isOnline,
                        batteryLevel = battery,
                        lastSeen = formatTimestamp(lastSeen)
                    )
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Device status listener cancelled: ${error.message}")
            }
        }
        
        ref.addValueEventListener(deviceStatusListener!!)
    }
    
    private fun listenToLocation(deviceId: String) {
        val ref = database.getReference("${ParentalControlApp.RealtimePaths.LOCATIONS}/$deviceId/current")
        
        locationListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lat = snapshot.child("latitude").getValue(Double::class.java)
                val lng = snapshot.child("longitude").getValue(Double::class.java)
                val timestamp = snapshot.child("timestamp").getValue(Long::class.java)
                
                _uiState.update {
                    it.copy(
                        latitude = lat,
                        longitude = lng,
                        locationUpdatedAt = formatTimestamp(timestamp)
                    )
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Location listener cancelled: ${error.message}")
            }
        }
        
        ref.addValueEventListener(locationListener!!)
    }
    
    private fun listenToNotificationCount(deviceId: String) {
        val ref = database.getReference("notifications/$deviceId/unreadCount")
        
        notificationCountListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val count = snapshot.getValue(Int::class.java) ?: 0
                _uiState.update { it.copy(unreadNotifications = count) }
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Notification count listener cancelled: ${error.message}")
            }
        }
        
        ref.addValueEventListener(notificationCountListener!!)
    }
    
    /**
     * Send a command to the child device
     */
    fun sendCommand(commandType: String, data: Map<String, Any> = emptyMap()) {
        viewModelScope.launch {
            try {
                val deviceId = _uiState.value.childDeviceId
                val commandRef = database.getReference("${ParentalControlApp.RealtimePaths.COMMANDS}/$deviceId").push()
                
                val command = hashMapOf<String, Any>(
                    "type" to commandType,
                    "status" to "pending",
                    "timestamp" to System.currentTimeMillis(),
                    "requestedBy" to "parent"
                )
                data.forEach { (key, value) -> command[key] = value }
                
                commandRef.setValue(command).await()
                Log.d(TAG, "Command sent: $commandType")
            } catch (e: Exception) {
                Log.e(TAG, "Error sending command", e)
            }
        }
    }
    
    private fun formatTimestamp(timestamp: Long?): String {
        if (timestamp == null) return "Unknown"
        
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        return when {
            diff < 60_000 -> "Just now"
            diff < 3600_000 -> "${diff / 60_000} min ago"
            diff < 86400_000 -> "${diff / 3600_000} hours ago"
            else -> SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(timestamp))
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        val deviceId = _uiState.value.childDeviceId
        
        deviceStatusListener?.let {
            database.getReference("${ParentalControlApp.RealtimePaths.DEVICES}/$deviceId/status")
                .removeEventListener(it)
        }
        locationListener?.let {
            database.getReference("${ParentalControlApp.RealtimePaths.LOCATIONS}/$deviceId/current")
                .removeEventListener(it)
        }
        notificationCountListener?.let {
            database.getReference("notifications/$deviceId/unreadCount")
                .removeEventListener(it)
        }
    }
}
