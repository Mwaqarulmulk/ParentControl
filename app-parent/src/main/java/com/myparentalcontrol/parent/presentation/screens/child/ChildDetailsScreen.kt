package com.myparentalcontrol.parent.presentation.screens.child

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Main monitoring dashboard for a specific child device
 * Shows all monitoring features and controls
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildDetailsScreen(
    childDeviceId: String,
    childName: String,
    onNavigateBack: () -> Unit,
    onNavigateToStream: (String, String) -> Unit,
    onNavigateToLocation: (String, String) -> Unit,
    onNavigateToNotifications: (String, String) -> Unit,
    onNavigateToSnapshots: (String, String) -> Unit,
    viewModel: ChildDetailsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(childDeviceId) {
        viewModel.loadChildDetails(childDeviceId)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(if (uiState.childName.isNotEmpty()) uiState.childName else childName)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Circle,
                                contentDescription = null,
                                tint = if (uiState.isOnline) Color.Green else Color.Gray,
                                modifier = Modifier.size(8.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (uiState.isOnline) "Online" else "Offline",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Battery indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(
                            imageVector = when {
                                uiState.batteryLevel > 80 -> Icons.Default.BatteryFull
                                uiState.batteryLevel > 50 -> Icons.Default.Battery5Bar
                                uiState.batteryLevel > 20 -> Icons.Default.Battery3Bar
                                else -> Icons.Default.Battery1Bar
                            },
                            contentDescription = "Battery",
                            tint = when {
                                uiState.batteryLevel > 50 -> Color.Green
                                uiState.batteryLevel > 20 -> Color(0xFFFF9800)
                                else -> Color.Red
                            }
                        )
                        Text("${uiState.batteryLevel}%")
                    }
                }
            )
        }
    ) { paddingValues ->
        val displayName = if (uiState.childName.isNotEmpty()) uiState.childName else childName
        
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Quick Status Card
            item {
                QuickStatusCard(uiState)
            }
            
            // Live Monitoring Section
            item {
                Text(
                    text = "Live Monitoring",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MonitoringButton(
                        title = "Camera",
                        subtitle = "Live view",
                        icon = Icons.Default.Videocam,
                        color = Color(0xFF2196F3),
                        enabled = uiState.isOnline,
                        onClick = { onNavigateToStream(childDeviceId, displayName) },
                        modifier = Modifier.weight(1f)
                    )
                    
                    MonitoringButton(
                        title = "Screen",
                        subtitle = "Mirror",
                        icon = Icons.Default.ScreenShare,
                        color = Color(0xFF9C27B0),
                        enabled = uiState.isOnline,
                        onClick = { onNavigateToStream(childDeviceId, displayName) },
                        modifier = Modifier.weight(1f)
                    )
                    
                    MonitoringButton(
                        title = "Audio",
                        subtitle = "Listen",
                        icon = Icons.Default.Mic,
                        color = Color(0xFFFF5722),
                        enabled = uiState.isOnline,
                        onClick = { onNavigateToStream(childDeviceId, displayName) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            // Location Section
            item {
                Text(
                    text = "Location",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            
            item {
                LocationCard(
                    latitude = uiState.latitude,
                    longitude = uiState.longitude,
                    lastUpdated = uiState.locationUpdatedAt,
                    onClick = { onNavigateToLocation(childDeviceId, displayName) }
                )
            }
            
            // Quick Actions Section
            item {
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionButton(
                        title = "Notifications",
                        icon = Icons.Default.Notifications,
                        badgeCount = uiState.unreadNotifications,
                        onClick = { onNavigateToNotifications(childDeviceId, displayName) },
                        modifier = Modifier.weight(1f)
                    )
                    
                    QuickActionButton(
                        title = "Snapshots",
                        icon = Icons.Default.CameraAlt,
                        badgeCount = 0,
                        onClick = { onNavigateToSnapshots(childDeviceId, displayName) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionButton(
                        title = "Take Snapshot",
                        icon = Icons.Default.PhotoCamera,
                        onClick = { viewModel.sendCommand("TAKE_CAMERA_SNAPSHOT") },
                        modifier = Modifier.weight(1f),
                        enabled = uiState.isOnline
                    )
                    
                    QuickActionButton(
                        title = "Screenshot",
                        icon = Icons.Default.Screenshot,
                        onClick = { viewModel.sendCommand("TAKE_SCREEN_SNAPSHOT") },
                        modifier = Modifier.weight(1f),
                        enabled = uiState.isOnline
                    )
                }
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionButton(
                        title = "Play Sound",
                        icon = Icons.Default.VolumeUp,
                        onClick = { viewModel.sendCommand("PLAY_SOUND") },
                        modifier = Modifier.weight(1f),
                        enabled = uiState.isOnline
                    )
                    
                    QuickActionButton(
                        title = "Refresh Location",
                        icon = Icons.Default.MyLocation,
                        onClick = { viewModel.sendCommand("UPDATE_LOCATION") },
                        modifier = Modifier.weight(1f),
                        enabled = uiState.isOnline
                    )
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun QuickStatusCard(uiState: ChildDetailsUiState) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Device Status",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatusItem(
                    label = "Last Seen",
                    value = uiState.lastSeen,
                    icon = Icons.Default.AccessTime
                )
                StatusItem(
                    label = "Battery",
                    value = "${uiState.batteryLevel}%",
                    icon = Icons.Default.BatteryStd
                )
                StatusItem(
                    label = "Network",
                    value = if (uiState.isOnline) "Connected" else "Offline",
                    icon = Icons.Default.Wifi
                )
            }
        }
    }
}

@Composable
private fun StatusItem(
    label: String,
    value: String,
    icon: ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MonitoringButton(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) color.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (enabled) color else Color.Gray),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LocationCard(
    latitude: Double?,
    longitude: Double?,
    lastUpdated: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF4CAF50)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Current Location",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (latitude != null && longitude != null) {
                    Text(
                        text = "%.4f, %.4f".format(latitude, longitude),
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Text(
                        text = "Location unavailable",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "Updated: $lastUpdated",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickActionButton(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    badgeCount: Int = 0
) {
    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
    ) {
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) MaterialTheme.colorScheme.primary else Color.Gray
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            
            if (badgeCount > 0) {
                Badge(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Text(badgeCount.toString())
                }
            }
        }
    }
}
