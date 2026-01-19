package com.myparentalcontrol.parent.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.myparentalcontrol.parent.data.model.ChildDevice
import com.myparentalcontrol.parent.data.model.LocationHistory
import com.myparentalcontrol.parent.data.model.NotificationData
import com.myparentalcontrol.shared.ParentalControlApp
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChildDeviceRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val realtimeDb: FirebaseDatabase
) {
    
    companion object {
        private const val TAG = "ChildDeviceRepository"
    }
    
    private var deviceListeners = mutableListOf<ListenerRegistration>()
    
    /**
     * Get real-time updates of all paired children
     * This properly listens to both the children list AND individual device updates
     */
    fun getPairedChildren(): Flow<List<ChildDevice>> = callbackFlow {
        val parentId = auth.currentUser?.uid ?: run {
            Log.e(TAG, "Not authenticated - no parent ID")
            close(Exception("Not authenticated"))
            return@callbackFlow
        }
        
        Log.d(TAG, "Loading paired children for parent: $parentId")
        
        // Clear any existing device listeners
        deviceListeners.forEach { it.remove() }
        deviceListeners.clear()
        
        // Keep track of current devices
        val currentDevices = mutableMapOf<String, ChildDevice>()
        
        // Listen to parent's children collection
        val childrenListener = firestore.collection("users")
            .document(parentId)
            .collection("children")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to children collection", error)
                    return@addSnapshotListener
                }
                
                val childIds = snapshot?.documents?.map { it.id } ?: emptyList()
                Log.d(TAG, "Children IDs found: $childIds")
                
                if (childIds.isEmpty()) {
                    Log.d(TAG, "No children found, emitting empty list")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                // Remove listeners for devices no longer in the list
                val removedIds = currentDevices.keys - childIds.toSet()
                removedIds.forEach { id ->
                    currentDevices.remove(id)
                }
                
                // Set up listeners for each child device for real-time updates
                childIds.forEach { deviceId ->
                    if (!currentDevices.containsKey(deviceId)) {
                        val deviceListener = firestore.collection(ParentalControlApp.FirebaseCollections.DEVICES)
                            .document(deviceId)
                            .addSnapshotListener { deviceSnapshot, deviceError ->
                                if (deviceError != null) {
                                    Log.e(TAG, "Error listening to device $deviceId", deviceError)
                                    return@addSnapshotListener
                                }
                                
                                val device = deviceSnapshot?.toObject(ChildDevice::class.java)
                                if (device != null) {
                                    Log.d(TAG, "Device updated: ${device.deviceName} (${device.deviceId})")
                                    currentDevices[deviceId] = device
                                } else {
                                    Log.d(TAG, "Device $deviceId removed or null")
                                    currentDevices.remove(deviceId)
                                }
                                
                                // Emit updated list
                                val devicesList = currentDevices.values.toList()
                                Log.d(TAG, "Emitting ${devicesList.size} devices")
                                trySend(devicesList)
                            }
                        deviceListeners.add(deviceListener)
                    }
                }
            }
        
        awaitClose { 
            Log.d(TAG, "Closing flow - removing listeners")
            childrenListener.remove()
            deviceListeners.forEach { it.remove() }
            deviceListeners.clear()
        }
    }
    
    /**
     * Get real-time updates for a specific child device
     */
    fun getChildDevice(deviceId: String): Flow<ChildDevice?> = callbackFlow {
        val listener = firestore.collection(ParentalControlApp.FirebaseCollections.DEVICES)
            .document(deviceId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val device = snapshot?.toObject(ChildDevice::class.java)
                trySend(device)
            }
        
        awaitClose { listener.remove() }
    }
    
    /**
     * Get INSTANT real-time device status from Realtime Database
     * This is faster than Firestore for real-time status updates
     */
    fun getDeviceStatusRealtime(deviceId: String): Flow<DeviceStatus> = callbackFlow {
        val statusRef = realtimeDb.getReference("devices/$deviceId/status")
        
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val isOnline = snapshot.child("isOnline").getValue(Boolean::class.java) ?: false
                val lastSeen = snapshot.child("lastSeen").getValue(Long::class.java) ?: 0L
                val batteryLevel = snapshot.child("batteryLevel").getValue(Int::class.java) ?: 0
                val isCharging = snapshot.child("isCharging").getValue(Boolean::class.java) ?: false
                val networkType = snapshot.child("networkType").getValue(String::class.java) ?: ""
                
                trySend(DeviceStatus(
                    isOnline = isOnline,
                    lastSeen = lastSeen,
                    batteryLevel = batteryLevel,
                    isCharging = isCharging,
                    networkType = networkType
                ))
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Realtime status listener cancelled", error.toException())
            }
        }
        
        statusRef.addValueEventListener(listener)
        awaitClose { statusRef.removeEventListener(listener) }
    }
    
    /**
     * Get real-time notifications from Realtime Database
     */
    fun getNotificationsRealtime(deviceId: String): Flow<List<NotificationData>> = callbackFlow {
        val notificationsRef = realtimeDb.getReference("notifications/$deviceId/history")
        
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val notifications = mutableListOf<NotificationData>()
                snapshot.children.forEach { child ->
                    try {
                        val data = NotificationData(
                            id = child.key ?: "",
                            packageName = child.child("packageName").getValue(String::class.java) ?: "",
                            appName = child.child("appName").getValue(String::class.java) ?: "",
                            title = child.child("title").getValue(String::class.java) ?: "",
                            text = child.child("text").getValue(String::class.java) ?: "",
                            timestamp = child.child("timestamp").getValue(Long::class.java) ?: 0L
                        )
                        notifications.add(data)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing notification", e)
                    }
                }
                // Sort by timestamp descending
                trySend(notifications.sortedByDescending { it.timestamp })
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Realtime notifications listener cancelled", error.toException())
            }
        }
        
        notificationsRef.addValueEventListener(listener)
        awaitClose { notificationsRef.removeEventListener(listener) }
    }
    
    /**
     * Get location history for a child device
     */
    suspend fun getLocationHistory(deviceId: String, limit: Int = 50): Result<List<LocationHistory>> {
        return try {
            val snapshot = firestore.collection(ParentalControlApp.FirebaseCollections.LOCATIONS)
                .document(deviceId)
                .collection("history")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()
            
            val locations = snapshot.documents.mapNotNull {
                it.toObject(LocationHistory::class.java)
            }
            
            Result.success(locations)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get notifications from child device
     */
    suspend fun getNotifications(deviceId: String, limit: Int = 100): Result<List<NotificationData>> {
        return try {
            val snapshot = firestore.collection("notifications")
                .document(deviceId)
                .collection("history")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()
            
            val notifications = snapshot.documents.mapNotNull {
                it.toObject(NotificationData::class.java)?.copy(id = it.id)
            }
            
            Result.success(notifications)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update child device nickname
     */
    suspend fun updateNickname(deviceId: String, nickname: String): Result<Unit> {
        return try {
            val parentId = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
            
            firestore.collection("users")
                .document(parentId)
                .collection("children")
                .document(deviceId)
                .update("nickname", nickname)
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Unpair (remove) a child device
     */
    suspend fun unpairDevice(deviceId: String): Result<Unit> {
        return try {
            val parentId = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
            
            // Remove from parent's children list
            firestore.collection("users")
                .document(parentId)
                .collection("children")
                .document(deviceId)
                .delete()
                .await()
            
            // Remove parent ID from child device
            firestore.collection(ParentalControlApp.FirebaseCollections.DEVICES)
                .document(deviceId)
                .update("parentId", null)
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Real-time device status from Firebase Realtime Database
 */
data class DeviceStatus(
    val isOnline: Boolean = false,
    val lastSeen: Long = 0L,
    val batteryLevel: Int = 0,
    val isCharging: Boolean = false,
    val networkType: String = ""
)
