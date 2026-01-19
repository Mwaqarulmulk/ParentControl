package com.myparentalcontrol.parent.presentation.screens.snapshots

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class SnapshotData(
    val id: String = "",
    val imageData: String = "", // Base64 encoded image
    val type: String = "", // "camera" or "screen"
    val timestamp: Long = 0L,
    val timeAgo: String = "",
    val formattedDate: String = "",
    val status: String = ""
)

enum class SnapshotFilter {
    ALL, CAMERA, SCREEN
}

data class SnapshotsUiState(
    val isLoading: Boolean = true,
    val isCapturing: Boolean = false,
    val snapshots: List<SnapshotData> = emptyList(),
    val filter: SnapshotFilter = SnapshotFilter.ALL,
    val message: String? = null,
    val error: String? = null
) {
    val filteredSnapshots: List<SnapshotData>
        get() = when (filter) {
            SnapshotFilter.ALL -> snapshots.filter { it.status == "completed" }
            SnapshotFilter.CAMERA -> snapshots.filter { it.type == "camera" && it.status == "completed" }
            SnapshotFilter.SCREEN -> snapshots.filter { it.type == "screen" && it.status == "completed" }
        }
}

@HiltViewModel
class SnapshotsViewModel @Inject constructor(
    private val database: FirebaseDatabase
) : ViewModel() {
    
    companion object {
        private const val TAG = "SnapshotsViewModel"
    }
    
    private val _uiState = MutableStateFlow(SnapshotsUiState())
    val uiState: StateFlow<SnapshotsUiState> = _uiState.asStateFlow()
    
    private var deviceId: String = ""
    private var snapshotsListener: ChildEventListener? = null
    
    fun loadSnapshots(childDeviceId: String) {
        deviceId = childDeviceId
        _uiState.update { it.copy(isLoading = true) }
        
        // Stop any existing listener
        stopListening()
        
        // Load snapshots from Realtime Database
        viewModelScope.launch {
            try {
                val ref = database.getReference("snapshots/$deviceId")
                    .orderByChild("timestamp")
                    .limitToLast(50)
                
                // Initial load
                val snapshot = ref.get().await()
                val snapshots = mutableListOf<SnapshotData>()
                
                snapshot.children.forEach { child ->
                    parseSnapshot(child)?.let { snapshots.add(it) }
                }
                
                snapshots.sortByDescending { it.timestamp }
                
                _uiState.update { 
                    it.copy(
                        snapshots = snapshots,
                        isLoading = false
                    )
                }
                
                // Start real-time listener
                startListening()
                
            } catch (e: Exception) {
                Log.e(TAG, "Error loading snapshots", e)
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "Failed to load snapshots: ${e.message}"
                    ) 
                }
            }
        }
    }
    
    private fun startListening() {
        val ref = database.getReference("snapshots/$deviceId")
            .orderByChild("timestamp")
            .limitToLast(50)
        
        snapshotsListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                parseSnapshot(snapshot)?.let { newSnapshot ->
                    _uiState.update { state ->
                        val updated = (listOf(newSnapshot) + state.snapshots)
                            .distinctBy { it.id }
                            .sortedByDescending { it.timestamp }
                        state.copy(snapshots = updated)
                    }
                }
            }
            
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                parseSnapshot(snapshot)?.let { updatedSnapshot ->
                    _uiState.update { state ->
                        val updated = state.snapshots.map { 
                            if (it.id == updatedSnapshot.id) updatedSnapshot else it 
                        }
                        state.copy(snapshots = updated)
                    }
                }
            }
            
            override fun onChildRemoved(snapshot: DataSnapshot) {
                val id = snapshot.key ?: return
                _uiState.update { state ->
                    state.copy(snapshots = state.snapshots.filter { it.id != id })
                }
            }
            
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Snapshots listener cancelled: ${error.message}")
            }
        }
        
        ref.addChildEventListener(snapshotsListener!!)
    }
    
    private fun stopListening() {
        snapshotsListener?.let {
            database.getReference("snapshots/$deviceId").removeEventListener(it)
        }
        snapshotsListener = null
    }
    
    private fun parseSnapshot(snapshot: DataSnapshot): SnapshotData? {
        return try {
            val id = snapshot.key ?: return null
            val type = snapshot.child("type").getValue(String::class.java) ?: ""
            val imageData = snapshot.child("imageData").getValue(String::class.java) ?: ""
            val timestamp = snapshot.child("timestamp").getValue(Long::class.java) ?: 0L
            val status = snapshot.child("status").getValue(String::class.java) ?: ""
            
            SnapshotData(
                id = id,
                imageData = imageData,
                type = type,
                timestamp = timestamp,
                timeAgo = formatTimeAgo(timestamp),
                formattedDate = formatDate(timestamp),
                status = status
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing snapshot", e)
            null
        }
    }
    
    fun takeCameraSnapshot(childDeviceId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCapturing = true) }
            
            try {
                // Send command to child device
                val commandRef = database.getReference("commands/$childDeviceId")
                val command = mapOf(
                    "type" to "TAKE_CAMERA_SNAPSHOT",
                    "timestamp" to System.currentTimeMillis(),
                    "status" to "pending",
                    "camera" to "back" // Default to back camera
                )
                
                commandRef.push().setValue(command).await()
                
                _uiState.update { 
                    it.copy(
                        isCapturing = false,
                        message = "Camera snapshot requested. It will appear shortly."
                    ) 
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error taking camera snapshot", e)
                _uiState.update { 
                    it.copy(
                        isCapturing = false,
                        message = "Failed to capture: ${e.message}"
                    ) 
                }
            }
        }
    }
    
    fun takeScreenshot(childDeviceId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCapturing = true) }
            
            try {
                // Send command to child device
                val commandRef = database.getReference("commands/$childDeviceId")
                val command = mapOf(
                    "type" to "TAKE_SCREEN_SNAPSHOT",
                    "timestamp" to System.currentTimeMillis(),
                    "status" to "pending"
                )
                
                commandRef.push().setValue(command).await()
                
                _uiState.update { 
                    it.copy(
                        isCapturing = false,
                        message = "Screenshot requested. It will appear shortly."
                    ) 
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error taking screenshot", e)
                _uiState.update { 
                    it.copy(
                        isCapturing = false,
                        message = "Failed to capture: ${e.message}"
                    ) 
                }
            }
        }
    }
    
    fun deleteSnapshot(snapshot: SnapshotData) {
        viewModelScope.launch {
            try {
                // Delete from Realtime Database
                database.getReference("snapshots/$deviceId/${snapshot.id}").removeValue().await()
                
                _uiState.update { state ->
                    state.copy(
                        snapshots = state.snapshots.filter { it.id != snapshot.id },
                        message = "Snapshot deleted"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting snapshot", e)
                _uiState.update { 
                    it.copy(message = "Failed to delete: ${e.message}")
                }
            }
        }
    }
    
    fun setFilter(filter: SnapshotFilter) {
        _uiState.update { it.copy(filter = filter) }
    }
    
    fun refresh() {
        loadSnapshots(deviceId)
    }
    
    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
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
                val sdf = java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault())
                sdf.format(java.util.Date(timestamp))
            }
        }
    }
    
    private fun formatDate(timestamp: Long): String {
        if (timestamp == 0L) return "Unknown"
        val sdf = java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }
    
    override fun onCleared() {
        super.onCleared()
        stopListening()
    }
}
