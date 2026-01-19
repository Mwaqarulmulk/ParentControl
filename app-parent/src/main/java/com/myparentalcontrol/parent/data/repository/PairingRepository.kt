package com.myparentalcontrol.parent.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.myparentalcontrol.parent.data.model.PairingCode
import com.myparentalcontrol.shared.ParentalControlApp
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PairingRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    
    companion object {
        private const val TAG = "PairingRepository"
        private const val TIMEOUT_MS = 30000L // 30 seconds timeout
    }
    
    /**
     * Generate a new pairing code for parent
     */
    suspend fun generatePairingCode(): Result<PairingCode> {
        return try {
            Log.d(TAG, "Generating pairing code...")
            
            val currentUser = auth.currentUser
            if (currentUser == null) {
                Log.e(TAG, "Not authenticated - no current user")
                return Result.failure(Exception("Not authenticated. Please sign in again."))
            }
            
            val parentId = currentUser.uid
            Log.d(TAG, "Parent ID: $parentId")
            
            val pairingCode = PairingCode.generate(parentId)
            Log.d(TAG, "Generated code: ${pairingCode.code}")
            
            // Convert to map for Firestore
            val codeData = hashMapOf(
                "code" to pairingCode.code,
                "parentId" to pairingCode.parentId,
                "createdAt" to pairingCode.createdAt,
                "expiresAt" to pairingCode.expiresAt,
                "isUsed" to pairingCode.isUsed,
                "childDeviceId" to (pairingCode.childDeviceId ?: "")
            )
            
            // Save to Firestore with timeout
            withTimeout(TIMEOUT_MS) {
                firestore.collection("pairingCodes")
                    .document(pairingCode.code)
                    .set(codeData)
                    .await()
            }
            
            Log.d(TAG, "Pairing code saved to Firestore successfully")
            Result.success(pairingCode)
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            Log.e(TAG, "Timeout while saving pairing code", e)
            Result.failure(Exception("Request timed out. Please check your internet connection."))
        } catch (e: Exception) {
            Log.e(TAG, "Error generating pairing code", e)
            Result.failure(Exception("Failed to generate code: ${e.message}"))
        }
    }
    
    /**
     * Check if a pairing code was used (child paired)
     */
    suspend fun checkCodeStatus(code: String): Result<PairingCode?> {
        return try {
            val doc = firestore.collection("pairingCodes")
                .document(code)
                .get()
                .await()
            
            if (!doc.exists()) {
                return Result.success(null)
            }
            
            val pairingCode = doc.toObject(PairingCode::class.java)
            Result.success(pairingCode)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking code status", e)
            Result.failure(e)
        }
    }
    
    /**
     * Verify a pairing code (called by child)
     */
    suspend fun verifyPairingCode(code: String, childDeviceId: String): Result<String> {
        return try {
            val docRef = firestore.collection("pairingCodes")
                .document(code)
                .get()
                .await()
            
            if (!docRef.exists()) {
                return Result.failure(Exception("Invalid code"))
            }
            
            val pairingCode = docRef.toObject(PairingCode::class.java)
                ?: return Result.failure(Exception("Invalid code"))
            
            if (!pairingCode.isValid()) {
                return Result.failure(Exception("Code expired or already used"))
            }
            
            // Mark code as used
            firestore.collection("pairingCodes")
                .document(code)
                .update(
                    mapOf(
                        "isUsed" to true,
                        "childDeviceId" to childDeviceId
                    )
                )
                .await()
            
            Result.success(pairingCode.parentId)
        } catch (e: Exception) {
            Log.e(TAG, "Error verifying pairing code", e)
            Result.failure(e)
        }
    }
    
    /**
     * Complete pairing - link devices
     */
    suspend fun completePairing(parentId: String, childDeviceId: String, childDeviceName: String): Result<Unit> {
        return try {
            // Update child device with parent ID
            firestore.collection(ParentalControlApp.FirebaseCollections.DEVICES)
                .document(childDeviceId)
                .update(
                    mapOf(
                        "parentId" to parentId,
                        "pairedAt" to System.currentTimeMillis()
                    )
                )
                .await()
            
            // Add to parent's children list
            firestore.collection("users")
                .document(parentId)
                .collection("children")
                .document(childDeviceId)
                .set(
                    mapOf(
                        "deviceId" to childDeviceId,
                        "deviceName" to childDeviceName,
                        "pairedAt" to System.currentTimeMillis()
                    )
                )
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error completing pairing", e)
            Result.failure(e)
        }
    }
}
