package com.myparentalcontrol.parent.ui.device

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.myparentalcontrol.parent.data.model.ChildDevice
import com.myparentalcontrol.parent.data.model.LocationHistory
import com.myparentalcontrol.parent.data.model.NotificationData
import com.myparentalcontrol.parent.data.repository.ChildDeviceRepository
import com.myparentalcontrol.parent.data.repository.CommandRepository
import com.myparentalcontrol.parent.data.repository.DeviceStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeviceDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val childDeviceRepository: ChildDeviceRepository,
    private val commandRepository: CommandRepository
) : ViewModel() {
    
    private val deviceId: String = savedStateHandle.get<String>("deviceId") ?: ""
    
    private val _childDevice = MutableStateFlow<ChildDevice?>(null)
    val childDevice: StateFlow<ChildDevice?> = _childDevice
    
    // Real-time status from Realtime Database (battery, online, network)
    private val _deviceStatus = MutableStateFlow<DeviceStatus?>(null)
    val deviceStatus: StateFlow<DeviceStatus?> = _deviceStatus
    
    private val _locationHistory = MutableStateFlow<List<LocationHistory>>(emptyList())
    val locationHistory: StateFlow<List<LocationHistory>> = _locationHistory
    
    private val _notifications = MutableStateFlow<List<NotificationData>>(emptyList())
    val notifications: StateFlow<List<NotificationData>> = _notifications
    
    private val _commandStatus = MutableStateFlow<CommandStatus>(CommandStatus.Idle)
    val commandStatus: StateFlow<CommandStatus> = _commandStatus
    
    init {
        loadDeviceData()
        observeRealtimeStatus()
    }
    
    private fun loadDeviceData() {
        viewModelScope.launch {
            // Load device info from Firestore
            childDeviceRepository.getChildDevice(deviceId)
                .collect { device ->
                    _childDevice.value = device
                }
        }
        
        viewModelScope.launch {
            // Load location history
            val result = childDeviceRepository.getLocationHistory(deviceId)
            result.onSuccess { locations ->
                _locationHistory.value = locations
            }
        }
        
        viewModelScope.launch {
            // Load notifications from Realtime Database for instant updates
            childDeviceRepository.getNotificationsRealtime(deviceId)
                .collect { notifications ->
                    _notifications.value = notifications
                }
        }
    }
    
    /**
     * Observe real-time device status from Realtime Database
     * This provides instant battery, online status, and network updates
     */
    private fun observeRealtimeStatus() {
        viewModelScope.launch {
            childDeviceRepository.getDeviceStatusRealtime(deviceId)
                .collect { status ->
                    _deviceStatus.value = status
                }
        }
    }
    
    fun refreshLocationHistory() {
        viewModelScope.launch {
            val result = childDeviceRepository.getLocationHistory(deviceId)
            result.onSuccess { locations ->
                _locationHistory.value = locations
            }
        }
    }
    
    fun refreshNotifications() {
        viewModelScope.launch {
            val result = childDeviceRepository.getNotifications(deviceId)
            result.onSuccess { notifications ->
                _notifications.value = notifications
            }
        }
    }
    
    // Command functions
    fun startCameraStream(cameraType: String = "front", withAudio: Boolean = false) {
        executeCommand {
            commandRepository.startCameraStream(deviceId, cameraType, withAudio)
        }
    }
    
    fun stopCameraStream() {
        executeCommand {
            commandRepository.stopCameraStream(deviceId)
        }
    }
    
    fun startScreenMirror(withAudio: Boolean = false) {
        executeCommand {
            commandRepository.startScreenMirror(deviceId, withAudio)
        }
    }
    
    fun stopScreenMirror() {
        executeCommand {
            commandRepository.stopScreenMirror(deviceId)
        }
    }
    
    fun startAudioStream() {
        executeCommand {
            commandRepository.startAudioStream(deviceId)
        }
    }
    
    fun stopAudioStream() {
        executeCommand {
            commandRepository.stopAudioStream(deviceId)
        }
    }
    
    fun requestLocationUpdate() {
        executeCommand {
            commandRepository.requestLocationUpdate(deviceId)
        }
    }
    
    fun ringDevice() {
        executeCommand {
            commandRepository.ringDevice(deviceId)
        }
    }
    
    private fun executeCommand(command: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            _commandStatus.value = CommandStatus.Loading
            val result = command()
            _commandStatus.value = if (result.isSuccess) {
                CommandStatus.Success
            } else {
                CommandStatus.Error(result.exceptionOrNull()?.message ?: "Command failed")
            }
            
            // Reset status after 3 seconds
            kotlinx.coroutines.delay(3000)
            _commandStatus.value = CommandStatus.Idle
        }
    }
    
    fun getCurrentLocation(): LatLng? {
        val device = _childDevice.value ?: return null
        return if (device.latitude != 0.0 && device.longitude != 0.0) {
            LatLng(device.latitude, device.longitude)
        } else {
            null
        }
    }
    
    // Snapshot commands
    fun takeCameraSnapshot() {
        executeCommand {
            commandRepository.takeCameraSnapshot(deviceId)
        }
    }
    
    fun takeScreenSnapshot() {
        executeCommand {
            commandRepository.takeScreenSnapshot(deviceId)
        }
    }
    
    // Recording commands
    fun startCameraRecording(duration: Int = 60) {
        executeCommand {
            commandRepository.startCameraRecording(deviceId, duration)
        }
    }
    
    fun stopCameraRecording() {
        executeCommand {
            commandRepository.stopCameraRecording(deviceId)
        }
    }
    
    fun startScreenRecording(duration: Int = 60) {
        executeCommand {
            commandRepository.startScreenRecording(deviceId, duration)
        }
    }
    
    fun stopScreenRecording() {
        executeCommand {
            commandRepository.stopScreenRecording(deviceId)
        }
    }
    
    fun startAmbientRecording(duration: Int = 60) {
        executeCommand {
            commandRepository.startAmbientRecording(deviceId, duration)
        }
    }
    
    fun stopAmbientRecording() {
        executeCommand {
            commandRepository.stopAmbientRecording(deviceId)
        }
    }
}

sealed class CommandStatus {
    object Idle : CommandStatus()
    object Loading : CommandStatus()
    object Success : CommandStatus()
    data class Error(val message: String) : CommandStatus()
}
