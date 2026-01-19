package com.myparentalcontrol.child.presentation.screens.status

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class StatusUiState(
    val isConnected: Boolean = false,
    val lastSyncTime: String = "",
    val isLocationTracking: Boolean = false,
    val isAppMonitoring: Boolean = false,
    val screenTimeToday: Int = 0,
    val deviceName: String = "",
    val batteryLevel: Int = 100,
    val isCharging: Boolean = false
)

@HiltViewModel
class StatusViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(StatusUiState())
    val uiState: StateFlow<StatusUiState> = _uiState.asStateFlow()
    
    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val batteryPct = (level * 100 / scale.toFloat()).toInt()
                val status = it.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL
                
                _uiState.update { state ->
                    state.copy(
                        batteryLevel = batteryPct,
                        isCharging = isCharging
                    )
                }
            }
        }
    }
    
    init {
        loadInitialState()
        registerBatteryReceiver()
    }
    
    private fun loadInitialState() {
        viewModelScope.launch {
            // Get device name
            val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"
            
            // Get initial battery level
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            
            // TODO: Load actual connection status from repository
            // For now, simulate connected state
            val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
            val lastSync = dateFormat.format(Date())
            
            _uiState.update {
                it.copy(
                    isConnected = true,
                    lastSyncTime = lastSync,
                    isLocationTracking = true,
                    isAppMonitoring = true,
                    screenTimeToday = 45, // TODO: Get from usage stats
                    deviceName = deviceName,
                    batteryLevel = batteryLevel
                )
            }
        }
    }
    
    private fun registerBatteryReceiver() {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(batteryReceiver, filter)
    }
    
    override fun onCleared() {
        super.onCleared()
        try {
            context.unregisterReceiver(batteryReceiver)
        } catch (e: Exception) {
            // Receiver might not be registered
        }
    }
}
