package com.myparentalcontrol.parent.presentation.screens.notifications

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

/**
 * Data class for notification display
 */
data class NotificationData(
    val id: String = "",
    val appName: String = "",
    val packageName: String = "",
    val title: String = "",
    val text: String = "",
    val bigText: String = "",
    val timestamp: Long = 0L,
    val timeAgo: String = "",
    val isOngoing: Boolean = false,
    val category: String = ""
)

data class NotificationsUiState(
    val isLoading: Boolean = true,
    val notifications: List<NotificationData> = emptyList(),
    val latestNotification: NotificationData? = null,
    val error: String? = null
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val database: FirebaseDatabase
) : ViewModel() {
    
    companion object {
        private const val TAG = "NotificationsViewModel"
    }
    
    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()
    
    private var deviceId: String = ""
    private var latestListener: ValueEventListener? = null
    private var historyListener: ChildEventListener? = null
    
    fun startListening(childDeviceId: String) {
        deviceId = childDeviceId
        _uiState.update { it.copy(isLoading = true) }
        
        listenToLatestNotification()
        listenToNotificationHistory()
    }
    
    private fun listenToLatestNotification() {
        val ref = database.getReference("notifications/$deviceId/latest")
        
        latestListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val notification = parseNotification(snapshot)
                    notification?.let { notif ->
                        _uiState.update { it.copy(latestNotification = notif) }
                    }
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Latest notification listener cancelled: ${error.message}")
            }
        }
        
        ref.addValueEventListener(latestListener!!)
    }
    
    private fun listenToNotificationHistory() {
        val ref = database.getReference("notifications/$deviceId/history")
            .orderByChild("timestamp")
            .limitToLast(100)
        
        historyListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val notification = parseNotification(snapshot)
                notification?.let { notif ->
                    _uiState.update { state ->
                        val updated = (listOf(notif) + state.notifications)
                            .distinctBy { it.id }
                            .sortedByDescending { it.timestamp }
                        state.copy(
                            notifications = updated,
                            isLoading = false
                        )
                    }
                }
            }
            
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                // Handle update if needed
            }
            
            override fun onChildRemoved(snapshot: DataSnapshot) {
                val id = snapshot.key ?: return
                _uiState.update { state ->
                    state.copy(
                        notifications = state.notifications.filter { it.id != id }
                    )
                }
            }
            
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "History listener cancelled: ${error.message}")
                _uiState.update { it.copy(isLoading = false, error = error.message) }
            }
        }
        
        ref.addChildEventListener(historyListener!!)
        
        // Also do initial load
        viewModelScope.launch {
            try {
                val snapshot = ref.get().await()
                val notifications = mutableListOf<NotificationData>()
                
                snapshot.children.forEach { child ->
                    parseNotification(child)?.let { notifications.add(it) }
                }
                
                _uiState.update {
                    it.copy(
                        notifications = notifications.sortedByDescending { n -> n.timestamp },
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading notifications", e)
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
    
    private fun parseNotification(snapshot: DataSnapshot): NotificationData? {
        return try {
            val id = snapshot.key ?: return null
            val appName = snapshot.child("appName").getValue(String::class.java) ?: ""
            val packageName = snapshot.child("packageName").getValue(String::class.java) ?: ""
            val title = snapshot.child("title").getValue(String::class.java) ?: ""
            val text = snapshot.child("text").getValue(String::class.java) ?: ""
            val bigText = snapshot.child("bigText").getValue(String::class.java) ?: ""
            val timestamp = snapshot.child("timestamp").getValue(Long::class.java) ?: 0L
            val isOngoing = snapshot.child("isOngoing").getValue(Boolean::class.java) ?: false
            val category = snapshot.child("category").getValue(String::class.java) ?: ""
            
            NotificationData(
                id = id,
                appName = appName,
                packageName = packageName,
                title = title,
                text = text,
                bigText = bigText,
                timestamp = timestamp,
                timeAgo = formatTimeAgo(timestamp),
                isOngoing = isOngoing,
                category = category
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing notification", e)
            null
        }
    }
    
    private fun formatTimeAgo(timestamp: Long): String {
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
    
    fun markAllAsRead() {
        viewModelScope.launch {
            try {
                database.getReference("notifications/$deviceId/unreadCount")
                    .setValue(0)
                    .await()
            } catch (e: Exception) {
                Log.e(TAG, "Error marking as read", e)
            }
        }
    }
    
    fun refresh() {
        _uiState.update { it.copy(isLoading = true, notifications = emptyList()) }
        stopListening()
        startListening(deviceId)
    }
    
    fun stopListening() {
        latestListener?.let {
            database.getReference("notifications/$deviceId/latest").removeEventListener(it)
        }
        historyListener?.let {
            database.getReference("notifications/$deviceId/history").removeEventListener(it)
        }
        latestListener = null
        historyListener = null
    }
    
    override fun onCleared() {
        super.onCleared()
        stopListening()
    }
}
