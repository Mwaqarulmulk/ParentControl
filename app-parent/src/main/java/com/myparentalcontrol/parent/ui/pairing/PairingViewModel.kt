package com.myparentalcontrol.parent.ui.pairing

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myparentalcontrol.parent.data.model.PairingCode
import com.myparentalcontrol.parent.data.repository.PairingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PairingViewModel @Inject constructor(
    private val pairingRepository: PairingRepository
) : ViewModel() {
    
    companion object {
        private const val TAG = "PairingViewModel"
    }
    
    private val _uiState = MutableStateFlow<PairingUiState>(PairingUiState.Initial)
    val uiState: StateFlow<PairingUiState> = _uiState
    
    // Event to notify when pairing is complete and navigation should happen
    private val _pairingCompleteEvent = MutableSharedFlow<String>()
    val pairingCompleteEvent: SharedFlow<String> = _pairingCompleteEvent
    
    private var countdownJob: Job? = null
    private var pollJob: Job? = null
    
    fun generatePairingCode() {
        Log.d(TAG, "generatePairingCode called")
        viewModelScope.launch {
            _uiState.value = PairingUiState.Loading
            Log.d(TAG, "State set to Loading")
            
            try {
                val result = pairingRepository.generatePairingCode()
                Log.d(TAG, "Repository returned result: ${result.isSuccess}")
                
                result.onSuccess { code ->
                    Log.d(TAG, "Code generated successfully: ${code.code}")
                    _uiState.value = PairingUiState.CodeGenerated(code)
                    startCountdown(code)
                    startPollingForPairing(code.code)
                }.onFailure { error ->
                    Log.e(TAG, "Failed to generate code", error)
                    _uiState.value = PairingUiState.Error(error.message ?: "Failed to generate code")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception while generating code", e)
                _uiState.value = PairingUiState.Error(e.message ?: "An unexpected error occurred")
            }
        }
    }
    
    private fun startCountdown(pairingCode: PairingCode) {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (!pairingCode.isExpired()) {
                delay(1000)
                // Only update if we're still showing this code
                val currentState = _uiState.value
                if (currentState is PairingUiState.CodeGenerated && currentState.code.code == pairingCode.code) {
                    _uiState.value = PairingUiState.CodeGenerated(pairingCode)
                }
            }
            // Code expired
            val currentState = _uiState.value
            if (currentState is PairingUiState.CodeGenerated && currentState.code.code == pairingCode.code) {
                _uiState.value = PairingUiState.CodeExpired
            }
        }
    }
    
    private fun startPollingForPairing(code: String) {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (true) {
                delay(2000) // Poll every 2 seconds for faster response
                
                val currentState = _uiState.value
                if (currentState !is PairingUiState.CodeGenerated) {
                    break
                }
                
                try {
                    val result = pairingRepository.checkCodeStatus(code)
                    result.onSuccess { pairingCode ->
                        if (pairingCode?.isUsed == true && !pairingCode.childDeviceId.isNullOrEmpty()) {
                            Log.d(TAG, "Device paired! Child ID: ${pairingCode.childDeviceId}")
                            countdownJob?.cancel()
                            
                            // Update UI state to show success
                            _uiState.value = PairingUiState.PairingSuccess(pairingCode.childDeviceId)
                            
                            // Emit event for navigation
                            _pairingCompleteEvent.emit(pairingCode.childDeviceId)
                            
                            return@launch
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error polling pairing status", e)
                }
            }
        }
    }
    
    fun cancelPairing() {
        countdownJob?.cancel()
        pollJob?.cancel()
        _uiState.value = PairingUiState.Initial
    }
    
    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
        pollJob?.cancel()
    }
}

sealed class PairingUiState {
    object Initial : PairingUiState()
    object Loading : PairingUiState()
    data class CodeGenerated(val code: PairingCode) : PairingUiState()
    object CodeExpired : PairingUiState()
    data class PairingSuccess(val childDeviceId: String) : PairingUiState()
    data class Error(val message: String) : PairingUiState()
}
