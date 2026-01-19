package com.myparentalcontrol.child.streaming.core

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
 * Manages Firebase Realtime Database signaling for WebRTC
 * Handles stream requests, offers, answers, and ICE candidates
 */
@Singleton
class SignalingManager @Inject constructor(
    private val database: FirebaseDatabase
) {
    companion object {
        private const val TAG = "SignalingManager"
    }
    
    private var deviceId: String = ""
    private var streamRequestListener: ValueEventListener? = null
    private var answerListener: ValueEventListener? = null
    private var parentIceCandidatesListener: ChildEventListener? = null
    
    /**
     * Initialize with device ID
     */
    fun initialize(deviceId: String) {
        this.deviceId = deviceId
        Log.d(TAG, "Initialized with device ID: $deviceId")
    }
    
    /**
     * Listen for stream requests from parent
     */
    fun observeStreamRequests(): Flow<StreamRequest?> = callbackFlow {
        val ref = database.getReference(SignalingConstants.getStreamRequestPath(deviceId))
        
        streamRequestListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    try {
                        @Suppress("UNCHECKED_CAST")
                        val map = snapshot.value as? Map<String, Any?>
                        if (map != null) {
                            val request = StreamRequest.fromMap(map)
                            Log.d(TAG, "Received stream request: $request")
                            trySend(request)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing stream request", e)
                    }
                } else {
                    trySend(null)
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Stream request listener cancelled: ${error.message}")
            }
        }
        
        ref.addValueEventListener(streamRequestListener!!)
        
        awaitClose {
            streamRequestListener?.let { ref.removeEventListener(it) }
        }
    }
    
    /**
     * Send offer to Firebase
     */
    suspend fun sendOffer(offer: SignalingData) {
        Log.d(TAG, "Sending offer")
        val ref = database.getReference(SignalingConstants.getOfferPath(deviceId))
        ref.setValue(offer.toMap()).await()
        Log.d(TAG, "Offer sent successfully")
    }
    
    /**
     * Listen for answer from parent
     */
    fun observeAnswer(): Flow<SignalingData?> = callbackFlow {
        val ref = database.getReference(SignalingConstants.getAnswerPath(deviceId))
        
        answerListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    try {
                        @Suppress("UNCHECKED_CAST")
                        val map = snapshot.value as? Map<String, Any?>
                        if (map != null) {
                            val answer = SignalingData.fromMap(map)
                            if (answer != null) {
                                Log.d(TAG, "Received answer")
                                trySend(answer)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing answer", e)
                    }
                } else {
                    trySend(null)
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Answer listener cancelled: ${error.message}")
            }
        }
        
        ref.addValueEventListener(answerListener!!)
        
        awaitClose {
            answerListener?.let { ref.removeEventListener(it) }
        }
    }
    
    /**
     * Send ICE candidate to Firebase
     */
    suspend fun sendIceCandidate(candidate: IceCandidateData) {
        Log.d(TAG, "Sending ICE candidate")
        val ref = database.getReference(SignalingConstants.getChildIceCandidatesPath(deviceId))
        ref.push().setValue(candidate.toMap()).await()
    }
    
    /**
     * Listen for ICE candidates from parent
     */
    fun observeParentIceCandidates(): Flow<IceCandidateData> = callbackFlow {
        val ref = database.getReference(SignalingConstants.getParentIceCandidatesPath(deviceId))
        
        parentIceCandidatesListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                try {
                    @Suppress("UNCHECKED_CAST")
                    val map = snapshot.value as? Map<String, Any?>
                    if (map != null) {
                        val candidate = IceCandidateData.fromMap(map)
                        if (candidate != null) {
                            Log.d(TAG, "Received parent ICE candidate")
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
        
        ref.addChildEventListener(parentIceCandidatesListener!!)
        
        awaitClose {
            parentIceCandidatesListener?.let { ref.removeEventListener(it) }
        }
    }
    
    /**
     * Update stream status
     */
    suspend fun updateStreamStatus(status: StreamStatus) {
        Log.d(TAG, "Updating stream status: $status")
        val ref = database.getReference(SignalingConstants.getStreamStatusPath(deviceId))
        ref.setValue(status.toMap()).await()
    }
    
    /**
     * Clear all signaling data for this device
     */
    suspend fun clearSignalingData() {
        Log.d(TAG, "Clearing signaling data")
        val ref = database.getReference(SignalingConstants.getDevicePath(deviceId))
        
        // Clear specific nodes but keep streamRequest for parent to see status
        ref.child(SignalingConstants.OFFER).removeValue().await()
        ref.child(SignalingConstants.ANSWER).removeValue().await()
        ref.child(SignalingConstants.CHILD_ICE_CANDIDATES).removeValue().await()
        ref.child(SignalingConstants.PARENT_ICE_CANDIDATES).removeValue().await()
        
        Log.d(TAG, "Signaling data cleared")
    }
    
    /**
     * Clear stream request (when stream ends)
     */
    suspend fun clearStreamRequest() {
        Log.d(TAG, "Clearing stream request")
        val ref = database.getReference(SignalingConstants.getStreamRequestPath(deviceId))
        ref.child(SignalingConstants.FIELD_IS_ACTIVE).setValue(false).await()
    }
    
    /**
     * Stop all listeners
     */
    fun stopListening() {
        Log.d(TAG, "Stopping all listeners")
        
        streamRequestListener?.let {
            database.getReference(SignalingConstants.getStreamRequestPath(deviceId))
                .removeEventListener(it)
        }
        streamRequestListener = null
        
        answerListener?.let {
            database.getReference(SignalingConstants.getAnswerPath(deviceId))
                .removeEventListener(it)
        }
        answerListener = null
        
        parentIceCandidatesListener?.let {
            database.getReference(SignalingConstants.getParentIceCandidatesPath(deviceId))
                .removeEventListener(it)
        }
        parentIceCandidatesListener = null
    }
}
