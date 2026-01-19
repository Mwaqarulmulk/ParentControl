package com.myparentalcontrol.child.ui.pairing

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myparentalcontrol.child.data.repository.ChildPairingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChildPairingViewModel @Inject constructor(
    private val pairingRepository: ChildPairingRepository
) : ViewModel() {
    
    companion object {
        private const val TAG = "ChildPairingViewModel"
    }
    
    private val _uiState = MutableStateFlow<ChildPairingUiState>(ChildPairingUiState.Initial)
    val uiState: StateFlow<ChildPairingUiState> = _uiState
    
    private val _isPaired = MutableStateFlow(false)
    val isPaired: StateFlow<Boolean> = _isPaired
    
    init {
        // Don't auto-check on init - let the splash screen handle it
    }
    
    /**
     * Check if device is paired - called from splash screen
     */
    fun checkPairingStatus() {
        viewModelScope.launch {
            Log.d(TAG, "Checking pairing status...")
            try {
                val paired = pairingRepository.isDevicePaired()
                Log.d(TAG, "Device paired: $paired")
                _isPaired.value = paired
                if (paired) {
                    _uiState.value = ChildPairingUiState.AlreadyPaired
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking pairing status", e)
                _isPaired.value = false
            }
        }
    }
    
    /**
     * Clear local pairing data and reset state
     */
    fun clearPairingData() {
        Log.d(TAG, "Clearing pairing data...")
        pairingRepository.clearPairingInfo()
        _isPaired.value = false
        _uiState.value = ChildPairingUiState.Initial
    }
    
    fun verifyAndPair(code: String) {
        // Validate code format
        val cleanCode = code.trim()
        if (cleanCode.length != 6 || !cleanCode.all { it.isDigit() }) {
            _uiState.value = ChildPairingUiState.Error("Please enter a valid 6-digit code")
            return
        }
        
        viewModelScope.launch {
            _uiState.value = ChildPairingUiState.Loading("Authenticating...")
            Log.d(TAG, "Starting pairing process with code: $cleanCode")
            
            try {
                // First ensure we're authenticated
                val authResult = pairingRepository.ensureAuthenticated()
                if (authResult.isFailure) {
                    val error = authResult.exceptionOrNull()?.message ?: "Authentication failed"
                    Log.e(TAG, "Authentication failed: $error")
                    _uiState.value = ChildPairingUiState.Error("Authentication failed: $error. Make sure Anonymous Auth is enabled in Firebase.")
                    return@launch
                }
                Log.d(TAG, "Authenticated successfully: ${authResult.getOrNull()}")
                
                val deviceId = pairingRepository.getDeviceId()
                Log.d(TAG, "Device ID: $deviceId")
                
                // Verify code
                _uiState.value = ChildPairingUiState.Loading("Verifying code...")
                val verifyResult = pairingRepository.verifyPairingCode(cleanCode, deviceId)
                
                if (verifyResult.isFailure) {
                    val errorMsg = verifyResult.exceptionOrNull()?.message ?: "Invalid or expired code"
                    Log.e(TAG, "Verification failed: $errorMsg")
                    _uiState.value = ChildPairingUiState.Error(errorMsg)
                    return@launch
                }
                
                val parentId = verifyResult.getOrThrow()
                Log.d(TAG, "Code verified, parent ID: $parentId")
                
                // Get device info
                val deviceName = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
                
                // Complete pairing
                _uiState.value = ChildPairingUiState.Loading("Setting up device...")
                val pairResult = pairingRepository.completePairing(parentId, deviceId, deviceName)
                
                if (pairResult.isSuccess) {
                    Log.d(TAG, "Pairing completed successfully")
                    _isPaired.value = true
                    _uiState.value = ChildPairingUiState.Success(parentId)
                } else {
                    val errorMsg = pairResult.exceptionOrNull()?.message ?: "Failed to complete pairing"
                    Log.e(TAG, "Pairing completion failed: $errorMsg")
                    _uiState.value = ChildPairingUiState.Error(errorMsg)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Exception during pairing", e)
                _uiState.value = ChildPairingUiState.Error(
                    "Pairing failed: ${e.message ?: "Unknown error"}"
                )
            }
        }
    }
    
    fun resetState() {
        _uiState.value = ChildPairingUiState.Initial
    }
}

sealed class ChildPairingUiState {
    object Initial : ChildPairingUiState()
    object AlreadyPaired : ChildPairingUiState()
    data class Loading(val message: String = "Please wait...") : ChildPairingUiState()
    data class Success(val parentId: String) : ChildPairingUiState()
    data class Error(val message: String) : ChildPairingUiState()
}
