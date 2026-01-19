package com.myparentalcontrol.child.ui.home

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myparentalcontrol.child.data.repository.ChildPairingRepository
import com.myparentalcontrol.child.util.DeviceUtils
import com.myparentalcontrol.child.util.MonitoringManager
import com.myparentalcontrol.shared.data.supabase.SupabaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DeviceStatusInfo(
    val batteryLevel: Int = 0,
    val isCharging: Boolean = false,
    val networkType: String = "unknown",
    val deviceName: String = "",
    val androidVersion: String = "",
    val isSyncing: Boolean = false,
    val lastSyncTime: Long = 0L
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val monitoringManager: MonitoringManager,
    private val pairingRepository: ChildPairingRepository,
    private val supabaseRepository: SupabaseRepository
) : ViewModel() {
    
    companion object {
        private const val TAG = "HomeViewModel"
    }
    
    private val _servicesStatus = MutableStateFlow<List<MonitoringManager.ServiceStatus>>(emptyList())
    val servicesStatus: StateFlow<List<MonitoringManager.ServiceStatus>> = _servicesStatus.asStateFlow()
    
    private val _isPaired = MutableStateFlow(false)
    val isPaired: StateFlow<Boolean> = _isPaired.asStateFlow()
    
    private val _deviceStatus = MutableStateFlow(DeviceStatusInfo())
    val deviceStatus: StateFlow<DeviceStatusInfo> = _deviceStatus.asStateFlow()
    
    init {
        checkPairingStatus()
    }
    
    private fun checkPairingStatus() {
        viewModelScope.launch {
            _isPaired.value = pairingRepository.isDevicePaired()
        }
    }
    
    /**
     * Start all monitoring services
     */
    fun startMonitoringServices() {
        Log.d(TAG, "Starting monitoring services")
        try {
            monitoringManager.startAllServices()
        } catch (e: Exception) {
            Log.e(TAG, "Error starting services", e)
        }
    }
    
    /**
     * Refresh services status and device info, sync to Supabase
     */
    fun refreshStatus(context: Context) {
        Log.d(TAG, "Refreshing services status")
        _servicesStatus.value = monitoringManager.getServicesStatus()
        
        // Update device status
        val batteryInfo = DeviceUtils.getBatteryInfo(context)
        val networkType = DeviceUtils.getNetworkType(context)
        val deviceId = DeviceUtils.getDeviceId(context)
        
        _deviceStatus.value = DeviceStatusInfo(
            batteryLevel = batteryInfo.level,
            isCharging = batteryInfo.isCharging,
            networkType = networkType,
            deviceName = DeviceUtils.getDeviceName(),
            androidVersion = DeviceUtils.getAndroidVersion(),
            isSyncing = true
        )
        
        // Sync to Supabase
        viewModelScope.launch {
            try {
                supabaseRepository.updateDeviceStatus(
                    deviceId = deviceId,
                    isOnline = true,
                    batteryLevel = batteryInfo.level,
                    isCharging = batteryInfo.isCharging,
                    networkType = networkType
                )
                Log.d(TAG, "Status synced to Supabase")
                _deviceStatus.value = _deviceStatus.value.copy(
                    isSyncing = false,
                    lastSyncTime = System.currentTimeMillis()
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing to Supabase", e)
                _deviceStatus.value = _deviceStatus.value.copy(isSyncing = false)
            }
        }
    }
    
    /**
     * Stop all monitoring services
     */
    fun stopMonitoringServices() {
        Log.d(TAG, "Stopping monitoring services")
        monitoringManager.stopAllServices()
        _servicesStatus.value = monitoringManager.getServicesStatus()
    }
}
