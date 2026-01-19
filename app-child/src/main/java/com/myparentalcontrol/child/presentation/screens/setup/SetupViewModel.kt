package com.myparentalcontrol.child.presentation.screens.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SetupUiState(
    val pairingCode: String = "",
    val isLoading: Boolean = false,
    val isConnected: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SetupViewModel @Inject constructor(
    // TODO: Inject PairingRepository when created
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()
    
    fun updatePairingCode(code: String) {
        // Only allow digits and limit to 6 characters
        val filteredCode = code.filter { it.isDigit() }.take(6)
        _uiState.update { it.copy(pairingCode = filteredCode, error = null) }
    }
    
    fun connect() {
        val code = _uiState.value.pairingCode
        if (code.length < 6) {
            _uiState.update { it.copy(error = "Please enter a 6-digit pairing code") }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                // TODO: Implement actual pairing logic with Firebase
                // For now, simulate a connection
                kotlinx.coroutines.delay(2000)
                
                // Simulate success for testing
                _uiState.update { it.copy(isLoading = false, isConnected = true) }
                
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        error = e.message ?: "Connection failed. Please try again."
                    ) 
                }
            }
        }
    }
}
