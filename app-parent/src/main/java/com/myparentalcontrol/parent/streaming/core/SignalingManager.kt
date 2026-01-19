package com.myparentalcontrol.parent.streaming.core

import android.util.Log
import com.google.firebase.database.*
import com.myparentalcontrol.shared.streaming.constants.SignalingConstants
import com.myparentalcontrol.shared.streaming.models.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages Firebase Realtime Database signaling for WebRTC on parent side
 * Sends stream requests and handles signaling with child device
 */
@Singleton
class SignalingManager @Inject constructor(
    private val database: FirebaseDatabase
) {
    companion object {
        private const val TAG = "ParentSignalingManager"
    }
    
    private var parentId: String = ""
    private var offerListener: ValueEventListener? = null
    private var childIceCandidatesListener: ChildEventListener? = null
    private var streamStatusListener: ValueEventListener? = null
    
    /**
     * Initialize with parent ID
     */
    fun initialize(parentId: String) {
        this.parentId = parentId
        Log.d(TAG, "Initialized with parent ID: $parentId")
    }
    
    /**
     * Send stream request to child device
     */
    suspend fun sendStreamRequest(childDeviceId: String, request: StreamRequest) {
        Log.d(TAG, "Sending stream request to $childDeviceId: $request")
        val ref = database.getReference(SignalingConstants.getStreamRequestPath(childDeviceId))
        ref.setValue(request.toMap()).await()
        Log.d(TAG, "Stream request sent successfully")
    }
    
    /**
     * Cancel stream request
     */
    suspend fun cancelStreamRequest(childDeviceId: String) {
        Log.d(TAG, "Cancelling stream request for $childDeviceId")
        val ref = database.getReference(SignalingConstants.getStreamRequestPath(childDeviceId))
        ref.child(SignalingConstants.FIELD_IS_ACTIVE).setValue(false).await()
        Log.d(TAG, "Stream request cancelled")
    }
    
    /**
     * Listen for offer from child
     */
    fun observeOffer(childDeviceId: String): Flow<SignalingData?> = callbackFlow {
        val ref = database.getReference(SignalingConstants.getOfferPath(childDeviceId))
        
        offerListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    try {
                        @Suppress("UNCHECKED_CAST")
                        val map = snapshot.value as? Map<String, Any?>
                        if (map != null) {
                            val offer = SignalingData.fromMap(map)
                            if (offer != null) {
                                Log.d(TAG, "Received offer from child")
                                trySend(offer)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing offer", e)
                    }
                } else {
                    trySend(null)
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Offer listener cancelled: ${error.message}")
            }
        }
        
        ref.addValueEventListener(offerListener!!)
        
        awaitClose {
            offerListener?.let { ref.removeEventListener(it) }
        }
    }
    
    /**
     * Send answer to child
     */
    suspend fun sendAnswer(childDeviceId: String, answer: SignalingData) {
        Log.d(TAG, "Sending answer to $childDeviceId")
        val ref = database.getReference(SignalingConstants.getAnswerPath(childDeviceId))
        ref.setValue(answer.toMap()).await()
        Log.d(TAG, "Answer sent successfully")
    }
    
    /**
     * Send ICE candidate to child
     */
    suspend fun sendIceCandidate(childDeviceId: String, candidate: IceCandidateData) {
        Log.d(TAG, "Sending ICE candidate to $childDeviceId")
        val ref = database.getReference(SignalingConstants.getParentIceCandidatesPath(childDeviceId))
        ref.push().setValue(candidate.toMap()).await()
    }
    
    /**
     * Listen for ICE candidates from child
     */
    fun observeChildIceCandidates(childDeviceId: String): Flow<IceCandidateData> = callbackFlow {
        val ref = database.getReference(SignalingConstants.getChildIceCandidatesPath(childDeviceId))
        
        childIceCandidatesListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                try {
                    @Suppress("UNCHECKED_CAST")
                    val map = snapshot.value as? Map<String, Any?>
                    if (map != null) {
                        val candidate = IceCandidateData.fromMap(map)
                        if (candidate != null) {
                            Log.d(TAG, "Received child ICE candidate")
                            trySend(candidate)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing ICE candidate", e)
                }
            }
            
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "ICE candidates listener cancelled: ${error.message}")
            }
        }
        
        ref.addChildEventListener(childIceCandidatesListener!!)
        
        awaitClose {
            childIceCandidatesListener?.let { ref.removeEventListener(it) }
        }
    }
    
    /**
     * Listen for stream status from child
     */
    fun observeStreamStatus(childDeviceId: String): Flow<StreamStatus> = callbackFlow {
        val ref = database.getReference(SignalingConstants.getStreamStatusPath(childDeviceId))
        
        streamStatusListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    try {
                        @Suppress("UNCHECKED_CAST")
                        val map = snapshot.value as? Map<String, Any?>
                        if (map != null) {
                            val status = StreamStatus.fromMap(map)
                            Log.d(TAG, "Received stream status: $status")
                            trySend(status)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing stream status", e)
                    }
                } else {
                    trySend(StreamStatus.disconnected())
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Stream status listener cancelled: ${error.message}")
            }
        }
        
        ref.addValueEventListener(streamStatusListener!!)
        
        awaitClose {
            streamStatusListener?.let { ref.removeEventListener(it) }
        }
    }
    
    /**
     * Clear signaling data for a device
     */
    suspend fun clearSignalingData(childDeviceId: String) {
        Log.d(TAG, "Clearing signaling data for $childDeviceId")
        val ref = database.getReference(SignalingConstants.getDevicePath(childDeviceId))
        
        // Clear parent-side data
        ref.child(SignalingConstants.ANSWER).removeValue().await()
        ref.child(SignalingConstants.PARENT_ICE_CANDIDATES).removeValue().await()
        
        Log.d(TAG, "Signaling data cleared")
    }
    
    /**
     * Stop all listeners for a device
     */
    fun stopListening(childDeviceId: String) {
        Log.d(TAG, "Stopping listeners for $childDeviceId")
        
        offerListener?.let {
            database.getReference(SignalingConstants.getOfferPath(childDeviceId))
                .removeEventListener(it)
        }
        offerListener = null
        
        childIceCandidatesListener?.let {
            database.getReference(SignalingConstants.getChildIceCandidatesPath(childDeviceId))
                .removeEventListener(it)
        }
        childIceCandidatesListener = null
        
        streamStatusListener?.let {
            database.getReference(SignalingConstants.getStreamStatusPath(childDeviceId))
                .removeEventListener(it)
        }
        streamStatusListener = null
    }
}
