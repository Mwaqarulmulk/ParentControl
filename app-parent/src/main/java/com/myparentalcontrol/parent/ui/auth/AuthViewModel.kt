package com.myparentalcontrol.parent.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myparentalcontrol.parent.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    private val _isAuthenticated = MutableStateFlow(authRepository.isLoggedIn)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()
    
    init {
        observeAuthState()
    }
    
    private fun observeAuthState() {
        viewModelScope.launch {
            authRepository.observeAuthState().collect { user ->
                _isAuthenticated.value = user != null
                if (user != null && _authState.value is AuthState.Loading) {
                    _authState.value = AuthState.Success
                }
            }
        }
    }
    
    fun signInWithEmail(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Please fill in all fields")
            return
        }
        
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            authRepository.signInWithEmail(email, password)
                .onSuccess {
                    _authState.value = AuthState.Success
                }
                .onFailure { error ->
                    _authState.value = AuthState.Error(formatErrorMessage(error))
                }
        }
    }
    
    fun registerWithEmail(email: String, password: String, confirmPassword: String, displayName: String) {
        if (email.isBlank() || password.isBlank() || displayName.isBlank()) {
            _authState.value = AuthState.Error("Please fill in all fields")
            return
        }
        
        if (password != confirmPassword) {
            _authState.value = AuthState.Error("Passwords do not match")
            return
        }
        
        if (password.length < 6) {
            _authState.value = AuthState.Error("Password must be at least 6 characters")
            return
        }
        
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            authRepository.registerWithEmail(email, password, displayName)
                .onSuccess {
                    _authState.value = AuthState.Success
                }
                .onFailure { error ->
                    _authState.value = AuthState.Error(formatErrorMessage(error))
                }
        }
    }
    
    fun signInAnonymously() {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            authRepository.signInAnonymously()
                .onSuccess {
                    _authState.value = AuthState.Success
                }
                .onFailure { error ->
                    _authState.value = AuthState.Error(formatErrorMessage(error))
                }
        }
    }
    
    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _authState.value = AuthState.Initial
        }
    }
    
    fun sendPasswordReset(email: String) {
        if (email.isBlank()) {
            _authState.value = AuthState.Error("Please enter your email")
            return
        }
        
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            authRepository.sendPasswordResetEmail(email)
                .onSuccess {
                    _authState.value = AuthState.PasswordResetSent
                }
                .onFailure { error ->
                    _authState.value = AuthState.Error(formatErrorMessage(error))
                }
        }
    }
    
    fun clearError() {
        if (_authState.value is AuthState.Error) {
            _authState.value = AuthState.Initial
        }
    }
    
    private fun formatErrorMessage(error: Throwable): String {
        return when {
            error.message?.contains("INVALID_EMAIL", ignoreCase = true) == true -> 
                "Invalid email address"
            error.message?.contains("WRONG_PASSWORD", ignoreCase = true) == true -> 
                "Incorrect password"
            error.message?.contains("USER_NOT_FOUND", ignoreCase = true) == true -> 
                "No account found with this email"
            error.message?.contains("EMAIL_EXISTS", ignoreCase = true) == true -> 
                "An account already exists with this email"
            error.message?.contains("WEAK_PASSWORD", ignoreCase = true) == true -> 
                "Password is too weak"
            error.message?.contains("NETWORK", ignoreCase = true) == true -> 
                "Network error. Please check your connection."
            else -> error.message ?: "An error occurred"
        }
    }
}

sealed class AuthState {
    object Initial : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    object PasswordResetSent : AuthState()
    data class Error(val message: String) : AuthState()
}
