package com.myparentalcontrol.parent.ui.snapshots

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.*
import com.myparentalcontrol.parent.data.repository.CommandRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SnapshotsViewModel @Inject constructor(
    private val database: FirebaseDatabase,
    private val commandRepository: CommandRepository
) : ViewModel() {

    private val _snapshots = MutableStateFlow<List<SnapshotData>>(emptyList())
    val snapshots: StateFlow<List<SnapshotData>> = _snapshots

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private var snapshotsListener: ValueEventListener? = null
    private var currentDeviceId: String? = null

    fun loadSnapshots(deviceId: String) {
        // Remove previous listener if any
        currentDeviceId?.let { id ->
            snapshotsListener?.let { listener ->
                database.getReference("snapshots/$id").removeEventListener(listener)
            }
        }

        currentDeviceId = deviceId
        _isLoading.value = true

        val ref = database.getReference("snapshots/$deviceId")
        
        snapshotsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val snapshotsList = mutableListOf<SnapshotData>()
                
                for (child in snapshot.children) {
                    val id = child.key ?: continue
                    val type = child.child("type").getValue(String::class.java) ?: ""
                    val url = child.child("url").getValue(String::class.java) ?: ""
                    val imageData = child.child("imageData").getValue(String::class.java) ?: ""
                    val status = child.child("status").getValue(String::class.java) ?: ""
                    val timestamp = child.child("timestamp").getValue(Long::class.java) ?: 0L
                    val message = child.child("message").getValue(String::class.java)
                    
                    snapshotsList.add(
                        SnapshotData(
                            id = id,
                            type = type,
                            url = url,
                            imageData = imageData,
                            status = status,
                            timestamp = timestamp,
                            message = message
                        )
                    )
                }
                
                // Sort by timestamp descending (newest first)
                _snapshots.value = snapshotsList.sortedByDescending { it.timestamp }
                _isLoading.value = false
            }

            override fun onCancelled(error: DatabaseError) {
                _isLoading.value = false
            }
        }

        ref.addValueEventListener(snapshotsListener!!)
    }

    fun requestCameraSnapshot(deviceId: String) {
        viewModelScope.launch {
            commandRepository.takeCameraSnapshot(deviceId)
        }
    }

    fun requestScreenSnapshot(deviceId: String) {
        viewModelScope.launch {
            commandRepository.takeScreenSnapshot(deviceId)
        }
    }

    fun deleteSnapshot(deviceId: String, snapshotId: String) {
        viewModelScope.launch {
            try {
                database.getReference("snapshots/$deviceId/$snapshotId").removeValue()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        currentDeviceId?.let { id ->
            snapshotsListener?.let { listener ->
                database.getReference("snapshots/$id").removeEventListener(listener)
            }
        }
    }
}
