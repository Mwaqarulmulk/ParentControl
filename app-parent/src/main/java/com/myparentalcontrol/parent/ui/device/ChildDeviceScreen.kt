package com.myparentalcontrol.parent.ui.device

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Child Device Screen - FlashGet Kids inspired design
 * Shows device status, live monitoring, location, and controls
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildDeviceScreen(
    deviceId: String,
    onNavigateBack: () -> Unit,
    onNavigateToLocation: (deviceId: String, deviceName: String) -> Unit = { _, _ -> },
    onNavigateToStream: (deviceId: String, deviceName: String, streamType: String) -> Unit = { _, _, _ -> },
    onNavigateToSnapshots: (deviceId: String, deviceName: String) -> Unit = { _, _ -> },
    onNavigateToScreenRecording: () -> Unit = {},
    onNavigateToCameraRecording: () -> Unit = {},
    onNavigateToAmbientRecording: () -> Unit = {},
    onNavigateToSnapshot: () -> Unit = {},
    onNavigateToUsageReport: () -> Unit = {},
    viewModel: DeviceDetailsViewModel = hiltViewModel()
) {
    val device by viewModel.childDevice.collectAsState()
    val deviceStatus by viewModel.deviceStatus.collectAsState()
    val currentLocation = viewModel.getCurrentLocation()
    
    // Use real-time status if available, fallback to device info
    val isOnline = deviceStatus?.isOnline ?: device?.isOnline ?: false
    val batteryLevel = deviceStatus?.batteryLevel ?: device?.batteryLevel ?: 0
    val isCharging = deviceStatus?.isCharging ?: device?.isCharging ?: false
    val networkType = deviceStatus?.networkType ?: device?.networkType ?: "unknown"
    
    Scaffold(
        containerColor = Color(0xFFF5F5F5)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header with gradient
            item {
                DeviceHeader(
                    deviceName = device?.deviceName ?: "Child Device",
                    isOnline = isOnline,
                    batteryLevel = batteryLevel,
                    isCharging = isCharging,
                    networkType = networkType,
                    onBack = onNavigateBack
                )
            }
            
            // Usage Report Section
            item {
                UsageReportCard(
                    screenTime = "0 min", // TODO: Implement actual screen time
                    onClick = onNavigateToUsageReport
                )
            }
            
            // Live Monitoring Section
            item {
                LiveMonitoringSection(
                    onRemoteCameraClick = {
                        device?.let { dev ->
                            onNavigateToStream(deviceId, dev.getDisplayName(), "camera")
                        }
                    },
                    onScreenMirroringClick = {
                        device?.let { dev ->
                            onNavigateToStream(deviceId, dev.getDisplayName(), "screen")
                        }
                    },
                    onOneWayAudioClick = {
                        device?.let { dev ->
                            onNavigateToStream(deviceId, dev.getDisplayName(), "audio")
                        }
                    }
                )
            }
            
            // Live Location Section
            item {
                LiveLocationSection(
                    currentLocation = currentLocation,
                    address = "", // TODO: Implement geocoding
                    lastUpdate = device?.lastSeen ?: 0L,
                    onClick = {
                        device?.let { dev ->
                            onNavigateToLocation(deviceId, dev.getDisplayName())
                        }
                    }
                )
            }
            
            // Snapshot & Recording Section
            item {
                SnapshotRecordingSection(
                    onCameraRecordingClick = onNavigateToCameraRecording,
                    onScreenRecordingClick = onNavigateToScreenRecording,
                    onAmbientRecordingClick = onNavigateToAmbientRecording,
                    onCameraSnapshotClick = {
                        // Take camera snapshot and navigate to view
                        viewModel.takeCameraSnapshot()
                        device?.let { dev ->
                            onNavigateToSnapshots(deviceId, dev.getDisplayName())
                        }
                    },
                    onScreenSnapshotClick = {
                        // Take screen snapshot and navigate to view
                        viewModel.takeScreenSnapshot()
                        device?.let { dev ->
                            onNavigateToSnapshots(deviceId, dev.getDisplayName())
                        }
                    }
                )
            }
            
            // Device Activity Section
            item {
                DeviceActivitySection(
                    viewModel = viewModel
                )
            }
            
            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun DeviceHeader(
    deviceName: String,
    isOnline: Boolean,
    batteryLevel: Int,
    isCharging: Boolean = false,
    networkType: String = "unknown",
    onBack: () -> Unit
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
            .padding(top = 16.dp, bottom = 24.dp)
    ) {
        Column {
            // Top bar with back button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { /* Add device */ }) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add",
                        tint = Color.White
                    )
                }
            }
            
            // Device info row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column {
                    Text(
                        text = deviceName,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Online status
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isOnline) Color(0xFF4CAF50) else Color.Gray)
                            )
                            Text(
                                text = if (isOnline) "Online" else "Offline",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 14.sp
                            )
                        }
                        
                        // Battery with charging indicator
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
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "$batteryLevel%",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 14.sp
                            )
                        }
                        
                        // Network type
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
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = when (networkType.lowercase()) {
                                    "wifi" -> "WiFi"
                                    "mobile" -> "Mobile"
                                    else -> "No Network"
                                },
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UsageReportCard(
    screenTime: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Usage Report",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    // Notification badge
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color.Red)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Screen Time: $screenTime",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
            
            // Chart icon placeholder
            Icon(
                Icons.Default.BarChart,
                contentDescription = null,
                tint = Color(0xFF7C4DFF),
                modifier = Modifier.size(48.dp)
            )
        }
    }
}

@Composable
private fun LiveMonitoringSection(
    onRemoteCameraClick: () -> Unit,
    onScreenMirroringClick: () -> Unit,
    onOneWayAudioClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Live Monitoring",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MonitoringItem(
                    icon = Icons.Default.Videocam,
                    label = "Remote Camera",
                    onClick = onRemoteCameraClick
                )
                MonitoringItem(
                    icon = Icons.Default.Smartphone,
                    label = "Screen Mirroring",
                    onClick = onScreenMirroringClick
                )
                MonitoringItem(
                    icon = Icons.Default.Headphones,
                    label = "One-Way Audio",
                    onClick = onOneWayAudioClick
                )
            }
        }
    }
}

@Composable
private fun MonitoringItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color(0xFFFFF3E0)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = Color(0xFFFF9800),
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun LiveLocationSection(
    currentLocation: LatLng?,
    address: String,
    lastUpdate: Long,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Live Location",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Open",
                    tint = Color.Gray
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Map preview
            if (currentLocation != null) {
                val cameraPositionState = rememberCameraPositionState {
                    position = CameraPosition.fromLatLngZoom(currentLocation, 15f)
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        uiSettings = MapUiSettings(
                            zoomControlsEnabled = false,
                            scrollGesturesEnabled = false,
                            zoomGesturesEnabled = false
                        )
                    ) {
                        Marker(
                            state = MarkerState(position = currentLocation),
                            title = "Current Location"
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Address and update time
                if (address.isNotEmpty()) {
                    Text(
                        text = address,
                        fontSize = 14.sp,
                        color = Color.DarkGray
                    )
                } else {
                    Text(
                        text = "Location: ${String.format("%.4f", currentLocation.latitude)}, ${String.format("%.4f", currentLocation.longitude)}",
                        fontSize = 14.sp,
                        color = Color.DarkGray
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Geofence",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = "Not Set",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Update,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Update",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = if (lastUpdate > 0) {
                                val diff = System.currentTimeMillis() - lastUpdate
                                when {
                                    diff < 60000 -> "Just now"
                                    diff < 3600000 -> "${diff / 60000} minutes ago"
                                    else -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(lastUpdate))
                                }
                            } else "Unknown",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFE0E0E0)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No location data", color = Color.Gray)
                }
            }
        }
    }
}

@Composable
private fun SnapshotRecordingSection(
    onCameraRecordingClick: () -> Unit,
    onScreenRecordingClick: () -> Unit,
    onAmbientRecordingClick: () -> Unit,
    onCameraSnapshotClick: () -> Unit,
    onScreenSnapshotClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Snapshot & Recording",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // First row - Recording options
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                RecordingItem(
                    icon = Icons.Default.Videocam,
                    label = "Camera\nRecording",
                    onClick = onCameraRecordingClick
                )
                RecordingItem(
                    icon = Icons.Default.ScreenshotMonitor,
                    label = "Screen\nRecording",
                    onClick = onScreenRecordingClick
                )
                RecordingItem(
                    icon = Icons.Default.Mic,
                    label = "Ambient\nRecording",
                    isNew = true,
                    onClick = onAmbientRecordingClick
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Second row - Snapshot options
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                RecordingItem(
                    icon = Icons.Default.CameraAlt,
                    label = "Camera\nSnapshot",
                    onClick = onCameraSnapshotClick
                )
                RecordingItem(
                    icon = Icons.Default.Smartphone,
                    label = "Screen\nSnapshot",
                    onClick = onScreenSnapshotClick
                )
                // Empty space for alignment
                Spacer(modifier = Modifier.width(80.dp))
            }
        }
    }
}

@Composable
private fun RecordingItem(
    icon: ImageVector,
    label: String,
    isNew: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFF3E0)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = label,
                    tint = Color(0xFFFF9800),
                    modifier = Modifier.size(28.dp)
                )
            }
            
            if (isNew) {
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd),
                    color = Color.Red,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "New",
                        color = Color.White,
                        fontSize = 8.sp,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Medium,
            lineHeight = 14.sp,
            maxLines = 2
        )
    }
}

@Composable
private fun DeviceActivitySection(
    viewModel: DeviceDetailsViewModel
) {
    val notifications by viewModel.notifications.collectAsState()
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Device Activity",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "See All",
                    color = Color(0xFF7C4DFF),
                    fontSize = 14.sp,
                    modifier = Modifier.clickable { }
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (notifications.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No recent activity",
                        color = Color.Gray
                    )
                }
            } else {
                notifications.take(5).forEach { notification ->
                    ActivityItem(
                        appName = notification.appName,
                        title = notification.title,
                        time = SimpleDateFormat("HH:mm", Locale.getDefault())
                            .format(Date(notification.timestamp))
                    )
                    if (notification != notifications.take(5).last()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityItem(
    appName: String,
    title: String,
    time: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE8EAF6)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Notifications,
                    contentDescription = null,
                    tint = Color(0xFF7C4DFF),
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(
                    text = appName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = title.take(30) + if (title.length > 30) "..." else "",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
        Text(
            text = time,
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}
