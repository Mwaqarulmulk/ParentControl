package com.myparentalcontrol.child.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val servicesStatus by viewModel.servicesStatus.collectAsState()
    val isPaired by viewModel.isPaired.collectAsState()
    val deviceStatus by viewModel.deviceStatus.collectAsState()
    
    // Start services and refresh status on launch
    LaunchedEffect(Unit) {
        viewModel.startMonitoringServices()
        viewModel.refreshStatus(context)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Child Device Monitor") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Device Status Header
            DeviceStatusHeader(
                deviceName = deviceStatus.deviceName,
                batteryLevel = deviceStatus.batteryLevel,
                isCharging = deviceStatus.isCharging,
                networkType = deviceStatus.networkType,
                isPaired = isPaired,
                isSyncing = deviceStatus.isSyncing,
                lastSyncTime = deviceStatus.lastSyncTime
            )
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Protection Status
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isPaired) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isPaired) Icons.Default.Shield else Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = if (isPaired) Color(0xFF4CAF50) else Color(0xFFFF9800)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = if (isPaired) "Protection Active" else "Not Paired",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isPaired) "This device is being monitored by your parent" 
                                       else "Pair with parent app to enable monitoring",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                // Active Services Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Active Services",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            
                            TextButton(
                                onClick = { viewModel.refreshStatus(context) }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Refresh")
                            }
                        }
                        
                        servicesStatus.forEach { status ->
                            FeatureItem(
                                icon = getIconForService(status.name),
                                text = status.name,
                                description = status.description,
                                isActive = status.isRunning,
                                permissionGranted = status.permissionGranted
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                val activeCount = servicesStatus.count { it.isRunning }
                Text(
                    text = "$activeCount of ${servicesStatus.size} services running in background",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DeviceStatusHeader(
    deviceName: String,
    batteryLevel: Int,
    isCharging: Boolean,
    networkType: String,
    isPaired: Boolean,
    isSyncing: Boolean = false,
    lastSyncTime: Long = 0L
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF7C4DFF),
                        Color(0xFF9575CD)
                    )
                )
            )
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Device Name
                Column {
                    Text(
                        text = deviceName.ifEmpty { "Child Device" },
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isPaired) Color(0xFF4CAF50) else Color.Gray)
                        )
                        Text(
                            text = if (isPaired) "Paired" else "Not Paired",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 12.sp
                        )
                    }
                }
                
                // Status Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Battery
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = when {
                                isCharging -> Icons.Default.BatteryChargingFull
                                batteryLevel > 80 -> Icons.Default.BatteryFull
                                batteryLevel > 50 -> Icons.Default.Battery5Bar
                                batteryLevel > 20 -> Icons.Default.Battery3Bar
                                else -> Icons.Default.BatteryAlert
                            },
                            contentDescription = "Battery",
                            tint = when {
                                isCharging -> Color(0xFF4CAF50)
                                batteryLevel <= 20 -> Color(0xFFFF5722)
                                else -> Color.White
                            },
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "$batteryLevel%",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                    
                    // Network
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = when (networkType.lowercase()) {
                                "wifi" -> Icons.Default.Wifi
                                "mobile" -> Icons.Default.SignalCellular4Bar
                                else -> Icons.Default.SignalCellularOff
                            },
                            contentDescription = "Network",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = when (networkType.lowercase()) {
                                "wifi" -> "WiFi"
                                "mobile" -> "Mobile"
                                else -> "None"
                            },
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                }
            }
            
            // Sync Status Row
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "Syncing...",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = "Synced",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (lastSyncTime > 0) "Synced" else "Ready to sync",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp
                        )
                    }
                }
                
                Text(
                    text = "Supabase + Firebase",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun FeatureItem(
    icon: ImageVector,
    text: String,
    description: String,
    isActive: Boolean,
    permissionGranted: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = if (!permissionGranted) "Permission required" else if (isActive) "Active" else "Inactive",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Icon(
            imageVector = if (isActive) Icons.Default.CheckCircle else if (!permissionGranted) Icons.Default.Warning else Icons.Default.Cancel,
            contentDescription = if (isActive) "Active" else "Inactive",
            modifier = Modifier.size(20.dp),
            tint = if (isActive) MaterialTheme.colorScheme.primary else if (!permissionGranted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun getIconForService(name: String): ImageVector {
    return when {
        name.contains("Camera", ignoreCase = true) -> Icons.Default.Videocam
        name.contains("Screen", ignoreCase = true) -> Icons.Default.ScreenShare
        name.contains("Audio", ignoreCase = true) -> Icons.Default.Mic
        name.contains("Notification", ignoreCase = true) -> Icons.Default.Notifications
        name.contains("Location", ignoreCase = true) -> Icons.Default.LocationOn
        else -> Icons.Default.Settings
    }
}
