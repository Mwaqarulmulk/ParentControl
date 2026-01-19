package com.myparentalcontrol.child.presentation.screens.status

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.myparentalcontrol.child.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusScreen(
    viewModel: StatusViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Connection Status Card
            item {
                ConnectionStatusCard(
                    isConnected = uiState.isConnected,
                    lastSync = uiState.lastSyncTime
                )
            }
            
            // Monitoring Status Cards
            item {
                Text(
                    text = "Monitoring Status",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            item {
                MonitoringStatusCard(
                    title = "Location Tracking",
                    description = if (uiState.isLocationTracking) 
                        "Your location is being shared" 
                    else 
                        "Location tracking is paused",
                    icon = Icons.Default.LocationOn,
                    isActive = uiState.isLocationTracking
                )
            }
            
            item {
                MonitoringStatusCard(
                    title = "App Usage Monitoring",
                    description = if (uiState.isAppMonitoring) 
                        "App usage is being monitored" 
                    else 
                        "App monitoring is paused",
                    icon = Icons.Default.Apps,
                    isActive = uiState.isAppMonitoring
                )
            }
            
            item {
                MonitoringStatusCard(
                    title = "Screen Time",
                    description = "Today: ${uiState.screenTimeToday} minutes",
                    icon = Icons.Default.Timer,
                    isActive = true
                )
            }
            
            // Device Info
            item {
                Text(
                    text = "Device Information",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            item {
                DeviceInfoCard(
                    deviceName = uiState.deviceName,
                    batteryLevel = uiState.batteryLevel,
                    isCharging = uiState.isCharging
                )
            }
        }
    }
}

@Composable
fun ConnectionStatusCard(
    isConnected: Boolean,
    lastSync: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isConnected) Icons.Default.CloudDone else Icons.Default.CloudOff,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = if (isConnected) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.error
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = if (isConnected) "Connected" else "Disconnected",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (isConnected) 
                        MaterialTheme.colorScheme.onPrimaryContainer 
                    else 
                        MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = if (lastSync.isNotEmpty()) "Last sync: $lastSync" else "Never synced",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isConnected) 
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) 
                    else 
                        MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun MonitoringStatusCard(
    title: String,
    description: String,
    icon: ImageVector,
    isActive: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = if (isActive) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .padding(2.dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = MaterialTheme.shapes.small,
                    color = if (isActive) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.outline
                ) {}
            }
        }
    }
}

@Composable
fun DeviceInfoCard(
    deviceName: String,
    batteryLevel: Int,
    isCharging: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.PhoneAndroid,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = deviceName,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isCharging) 
                        Icons.Default.BatteryChargingFull 
                    else if (batteryLevel > 20) 
                        Icons.Default.BatteryFull 
                    else 
                        Icons.Default.BatteryAlert,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = when {
                        batteryLevel <= 20 -> MaterialTheme.colorScheme.error
                        isCharging -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "$batteryLevel%${if (isCharging) " (Charging)" else ""}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
