package com.myparentalcontrol.parent.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    
    /**
     * Get current user
     */
    val currentUser: FirebaseUser?
        get() = auth.currentUser
    
    /**
     * Check if user is logged in
     */
    val isLoggedIn: Boolean
        get() = auth.currentUser != null
    
    /**
     * Observe authentication state changes
     */
    fun observeAuthState(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }
    
    /**
     * Sign in with email and password
     */
    suspend fun signInWithEmail(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user ?: return Result.failure(Exception("Sign in failed: No user returned"))
            
            // Ensure user document exists (for pairing flow)
            ensureUserDocument(user.uid)
            
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Ensure user document exists in Firestore (for pairing)
     */
    private suspend fun ensureUserDocument(uid: String) {
        try {
            val userDoc = firestore.collection("users").document(uid).get().await()
            if (!userDoc.exists()) {
                firestore.collection("users")
                    .document(uid)
                    .set(mapOf(
                        "type" to "parent",
                        "createdAt" to System.currentTimeMillis()
                    ))
                    .await()
            }
        } catch (e: Exception) {
            // Non-critical, log and continue
            android.util.Log.w("AuthRepository", "Failed to ensure user document", e)
        }
    }
    
    /**
     * Register with email and password
     */
    suspend fun registerWithEmail(email: String, password: String, displayName: String): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user ?: return Result.failure(Exception("Registration failed: No user returned"))
            
            // Update display name
            val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .build()
            user.updateProfile(profileUpdates).await()
            
            // Create parent profile in Firestore
            val parentProfile = hashMapOf(
                "uid" to user.uid,
                "email" to email,
                "displayName" to displayName,
                "createdAt" to System.currentTimeMillis(),
                "updatedAt" to System.currentTimeMillis()
            )
            
            firestore.collection("parents")
                .document(user.uid)
                .set(parentProfile)
                .await()
            
            // Also create user document for pairing flow
            firestore.collection("users")
                .document(user.uid)
                .set(mapOf(
                    "type" to "parent",
                    "createdAt" to System.currentTimeMillis()
                ))
                .await()
            
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Sign in anonymously (for quick testing)
     */
    suspend fun signInAnonymously(): Result<FirebaseUser> {
        return try {
            val result = auth.signInAnonymously().await()
            val user = result.user ?: return Result.failure(Exception("Anonymous sign in failed"))
            
            // Create user document for pairing flow
            firestore.collection("users")
                .document(user.uid)
                .set(mapOf(
                    "type" to "parent",
                    "createdAt" to System.currentTimeMillis(),
                    "isAnonymous" to true
                ))
                .await()
            
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Sign out
     */
    suspend fun signOut(): Result<Unit> {
        return try {
            auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Send password reset email
     */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
