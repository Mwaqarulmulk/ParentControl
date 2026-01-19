package com.myparentalcontrol.child.ui.permissions

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myparentalcontrol.child.util.PermissionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PermissionsViewModel @Inject constructor(
    val permissionManager: PermissionManager
) : ViewModel() {
    
    companion object {
        private const val TAG = "PermissionsViewModel"
    }
    
    private val _uiState = MutableStateFlow<PermissionsUiState>(PermissionsUiState.Loading)
    val uiState: StateFlow<PermissionsUiState> = _uiState.asStateFlow()
    
    private val _permissionsList = MutableStateFlow<List<PermissionManager.PermissionInfo>>(emptyList())
    val permissionsList: StateFlow<List<PermissionManager.PermissionInfo>> = _permissionsList.asStateFlow()
    
    private val _notificationListenerEnabled = MutableStateFlow(false)
    val notificationListenerEnabled: StateFlow<Boolean> = _notificationListenerEnabled.asStateFlow()
    
    private val _batteryOptimizationDisabled = MutableStateFlow(false)
    val batteryOptimizationDisabled: StateFlow<Boolean> = _batteryOptimizationDisabled.asStateFlow()
    
    private val _currentStep = MutableStateFlow(PermissionStep.RUNTIME_PERMISSIONS)
    val currentStep: StateFlow<PermissionStep> = _currentStep.asStateFlow()
    
    /**
     * Update permission status - call this after returning from permission requests
     */
    fun refreshPermissionStatus(context: android.content.Context) {
        viewModelScope.launch {
            Log.d(TAG, "Refreshing permission status")
            
            val permissions = permissionManager.getPermissionStatus(context)
            _permissionsList.value = permissions
            
            val allRuntimeGranted = permissions.all { it.isGranted }
            _notificationListenerEnabled.value = permissionManager.isNotificationListenerEnabled(context)
            _batteryOptimizationDisabled.value = permissionManager.isBatteryOptimizationDisabled(context)
            
            Log.d(TAG, "Runtime permissions granted: $allRuntimeGranted")
            Log.d(TAG, "Notification listener: ${_notificationListenerEnabled.value}")
            Log.d(TAG, "Battery optimization disabled: ${_batteryOptimizationDisabled.value}")
            
            // Determine current step and state
            when {
                !allRuntimeGranted -> {
                    _currentStep.value = PermissionStep.RUNTIME_PERMISSIONS
                    _uiState.value = PermissionsUiState.NeedPermissions(
                        grantedCount = permissions.count { it.isGranted },
                        totalCount = permissions.size
                    )
                }
                !_notificationListenerEnabled.value -> {
                    _currentStep.value = PermissionStep.NOTIFICATION_LISTENER
                    _uiState.value = PermissionsUiState.NeedNotificationListener
                }
                !_batteryOptimizationDisabled.value -> {
                    _currentStep.value = PermissionStep.BATTERY_OPTIMIZATION
                    _uiState.value = PermissionsUiState.NeedBatteryOptimization
                }
                else -> {
                    _currentStep.value = PermissionStep.COMPLETE
                    _uiState.value = PermissionsUiState.AllGranted
                }
            }
        }
    }
    
    /**
     * Check if all permissions are granted
     */
    fun areAllPermissionsGranted(context: android.content.Context): Boolean {
        val runtimeGranted = permissionManager.areAllBasicPermissionsGranted(context)
        val notificationListenerEnabled = permissionManager.isNotificationListenerEnabled(context)
        // Battery optimization is optional for initial setup
        return runtimeGranted && notificationListenerEnabled
    }
    
    /**
     * Skip battery optimization (optional)
     */
    fun skipBatteryOptimization() {
        _currentStep.value = PermissionStep.COMPLETE
        _uiState.value = PermissionsUiState.AllGranted
    }
    
    /**
     * Get permissions that need to be requested
     */
    fun getMissingPermissions(): Array<String> {
        return _permissionsList.value
            .filter { !it.isGranted }
            .map { it.permission }
            .toTypedArray()
    }
}

sealed class PermissionsUiState {
    object Loading : PermissionsUiState()
    data class NeedPermissions(val grantedCount: Int, val totalCount: Int) : PermissionsUiState()
    object NeedNotificationListener : PermissionsUiState()
    object NeedBatteryOptimization : PermissionsUiState()
    object AllGranted : PermissionsUiState()
}

enum class PermissionStep {
    RUNTIME_PERMISSIONS,
    NOTIFICATION_LISTENER,
    BATTERY_OPTIMIZATION,
    COMPLETE
}
