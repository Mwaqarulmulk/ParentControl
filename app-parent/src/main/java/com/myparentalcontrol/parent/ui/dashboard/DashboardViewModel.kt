package com.myparentalcontrol.parent.ui.dashboard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.myparentalcontrol.parent.data.model.ChildDevice
import com.myparentalcontrol.parent.data.repository.ChildDeviceRepository
import com.myparentalcontrol.shared.data.supabase.SupabaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val childDeviceRepository: ChildDeviceRepository,
    private val supabaseRepository: SupabaseRepository,
    private val auth: FirebaseAuth
) : ViewModel() {
    
    companion object {
        private const val TAG = "DashboardViewModel"
    }
    
    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState
    
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing
    
    init {
        loadPairedDevices()
    }
    
    private fun loadPairedDevices() {
        viewModelScope.launch {
            childDeviceRepository.getPairedChildren()
                .catch { error ->
                    Log.e(TAG, "Error loading devices", error)
                    _uiState.value = DashboardUiState.Error(error.message ?: "Failed to load devices")
                }
                .collect { children ->
                    Log.d(TAG, "Loaded ${children.size} children")
                    _uiState.value = if (children.isEmpty()) {
                        DashboardUiState.Empty
                    } else {
                        DashboardUiState.Success(children)
                    }
                }
        }
        
        // Also try loading from Supabase as backup
        viewModelScope.launch {
            try {
                val parentId = auth.currentUser?.uid ?: return@launch
                val result = supabaseRepository.getDevicesForParent(parentId)
                result.onSuccess { devices ->
                    Log.d(TAG, "Supabase: Loaded ${devices.size} devices")
                }
                result.onFailure { error ->
                    Log.e(TAG, "Supabase error: ${error.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading from Supabase", e)
            }
        }
    }
    
    fun refresh() {
        _isRefreshing.value = true
        viewModelScope.launch {
            try {
                loadPairedDevices()
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}

sealed class DashboardUiState {
    object Loading : DashboardUiState()
    object Empty : DashboardUiState()
    data class Success(val children: List<ChildDevice>) : DashboardUiState()
    data class Error(val message: String) : DashboardUiState()
}
