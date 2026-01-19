package com.myparentalcontrol.child.data.repository

import android.content.Context
import android.provider.Settings
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.myparentalcontrol.shared.ParentalControlApp
import com.myparentalcontrol.shared.data.supabase.SupabaseRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for handling child device pairing operations
 * Syncs to both Firebase (primary) and Supabase (secondary)
 */
@Singleton
class ChildPairingRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val supabaseRepository: SupabaseRepository
) {
    
    companion object {
        private const val TAG = "ChildPairingRepository"
    }
    
    /**
     * Ensure the child device is authenticated with Firebase
     * Uses anonymous authentication for child devices
     */
    suspend fun ensureAuthenticated(): Result<String> {
        return try {
            // Check if already authenticated
            val currentUser = auth.currentUser
            if (currentUser != null) {
                Log.d(TAG, "Already authenticated: ${currentUser.uid}")
                return Result.success(currentUser.uid)
            }
            
            // Sign in anonymously
            Log.d(TAG, "Signing in anonymously...")
            val result = auth.signInAnonymously().await()
            val user = result.user
            
            if (user != null) {
                Log.d(TAG, "Anonymous sign-in successful: ${user.uid}")
                Result.success(user.uid)
            } else {
                Log.e(TAG, "Anonymous sign-in returned null user")
                Result.failure(Exception("Authentication failed"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Authentication error", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get or create the device ID for this child device
     */
    fun getDeviceId(): String {
        return Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        )
    }
    
    /**
     * Verify a pairing code and get the parent ID
     */
    suspend fun verifyPairingCode(code: String, childDeviceId: String): Result<String> {
        return try {
            // Ensure authenticated first
            val authResult = ensureAuthenticated()
            if (authResult.isFailure) {
                return Result.failure(authResult.exceptionOrNull() ?: Exception("Authentication failed"))
            }
            
            Log.d(TAG, "Verifying pairing code: $code")
            
            val docRef = firestore.collection("pairingCodes")
                .document(code)
                .get()
                .await()
            
            if (!docRef.exists()) {
                Log.e(TAG, "Pairing code not found")
                return Result.failure(Exception("Invalid code. Please check and try again."))
            }
            
            val data = docRef.data ?: return Result.failure(Exception("Invalid code data"))
            
            val expiresAt = data["expiresAt"] as? Long ?: 0L
            val isUsed = data["isUsed"] as? Boolean ?: false
            val parentId = data["parentId"] as? String 
            
            if (parentId.isNullOrEmpty()) {
                return Result.failure(Exception("Invalid pairing code"))
            }
            
            // Check if expired
            if (System.currentTimeMillis() > expiresAt) {
                Log.e(TAG, "Pairing code expired")
                return Result.failure(Exception("Code expired. Please ask parent to generate a new code."))
            }
            
            // Check if already used
            if (isUsed) {
                Log.e(TAG, "Pairing code already used")
                return Result.failure(Exception("Code already used. Please ask parent to generate a new code."))
            }
            
            Log.d(TAG, "Pairing code valid, parent ID: $parentId")
            
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
            
            Log.d(TAG, "Pairing code marked as used")
            Result.success(parentId)
        } catch (e: Exception) {
            Log.e(TAG, "Error verifying pairing code", e)
            Result.failure(Exception("Failed to verify code: ${e.message}"))
        }
    }
    
    /**
     * Complete pairing - link this child device with the parent
     */
    suspend fun completePairing(parentId: String, childDeviceId: String, childDeviceName: String): Result<Unit> {
        return try {
            Log.d(TAG, "Completing pairing: parentId=$parentId, deviceId=$childDeviceId")
            
            // Create or update child device document
            val deviceData = hashMapOf(
                "deviceId" to childDeviceId,
                "parentId" to parentId,
                "deviceName" to childDeviceName,
                "deviceModel" to android.os.Build.MODEL,
                "deviceManufacturer" to android.os.Build.MANUFACTURER,
                "androidVersion" to android.os.Build.VERSION.RELEASE,
                "isOnline" to true,
                "lastSeen" to System.currentTimeMillis(),
                "batteryLevel" to 0,
                "isCharging" to false,
                "networkType" to "",
                "latitude" to 0.0,
                "longitude" to 0.0,
                "locationUpdatedAt" to 0L,
                "notificationAccessEnabled" to false,
                "pairedAt" to System.currentTimeMillis(),
                "nickname" to ""
            )
            
            // Save device to Firestore
            firestore.collection(ParentalControlApp.FirebaseCollections.DEVICES)
                .document(childDeviceId)
                .set(deviceData)
                .await()
            
            Log.d(TAG, "Device document created")
            
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
            
            Log.d(TAG, "Added to parent's children list")
            
            // Also sync to Supabase
            try {
                supabaseRepository.registerDevice(
                    deviceId = childDeviceId,
                    deviceName = childDeviceName,
                    deviceModel = android.os.Build.MODEL,
                    androidVersion = android.os.Build.VERSION.RELEASE,
                    appVersion = null
                )
                supabaseRepository.pairDeviceWithParent(childDeviceId, parentId)
                Log.d(TAG, "Device synced to Supabase")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync to Supabase (non-critical)", e)
                // Don't fail pairing if Supabase sync fails
            }
            
            // Save pairing info locally
            savePairingInfo(parentId, childDeviceId)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error completing pairing", e)
            Result.failure(Exception("Failed to complete pairing: ${e.message}"))
        }
    }
    
    /**
     * Save pairing info to SharedPreferences
     */
    private fun savePairingInfo(parentId: String, deviceId: String) {
        val prefs = context.getSharedPreferences("child_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("parent_id", parentId)
            .putString("device_id", deviceId)
            .putBoolean("is_paired", true)
            .putLong("paired_at", System.currentTimeMillis())
            .apply()
    }
    
    /**
     * Get saved pairing info from SharedPreferences
     */
    fun getSavedPairingInfo(): PairingInfo? {
        val prefs = context.getSharedPreferences("child_prefs", Context.MODE_PRIVATE)
        val isPaired = prefs.getBoolean("is_paired", false)
        
        if (!isPaired) return null
        
        val parentId = prefs.getString("parent_id", null) ?: return null
        val deviceId = prefs.getString("device_id", null) ?: return null
        
        return PairingInfo(parentId, deviceId)
    }
    
    /**
     * Check if this device is already paired (from local storage first, then Firebase)
     */
    suspend fun isDevicePaired(): Boolean {
        // First check local storage (faster)
        val localPairing = getSavedPairingInfo()
        if (localPairing != null) {
            return true
        }
        
        // Then verify with Firebase
        return try {
            // Ensure authenticated
            ensureAuthenticated()
            
            val deviceId = getDeviceId()
            val doc = firestore.collection(ParentalControlApp.FirebaseCollections.DEVICES)
                .document(deviceId)
                .get()
                .await()
            
            if (doc.exists()) {
                val parentId = doc.getString("parentId")
                val isPaired = !parentId.isNullOrEmpty()
                
                // Save locally if paired
                if (isPaired && parentId != null) {
                    savePairingInfo(parentId, deviceId)
                }
                
                isPaired
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking pairing status", e)
            false
        }
    }
    
    /**
     * Clear pairing info (for unpair functionality)
     */
    fun clearPairingInfo() {
        val prefs = context.getSharedPreferences("child_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}

/**
 * Data class for pairing info
 */
data class PairingInfo(
    val parentId: String,
    val deviceId: String
)
