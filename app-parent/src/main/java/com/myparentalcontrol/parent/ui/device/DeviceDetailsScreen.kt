package com.myparentalcontrol.parent.ui.device

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDetailsScreen(
    deviceId: String,
    onNavigateBack: () -> Unit,
    onNavigateToLocation: (deviceId: String, deviceName: String) -> Unit = { _, _ -> },
    onNavigateToStream: (deviceId: String, deviceName: String, streamType: String) -> Unit = { _, _, _ -> },
    viewModel: DeviceDetailsViewModel = hiltViewModel()
) {
    val device by viewModel.childDevice.collectAsState()
    val commandStatus by viewModel.commandStatus.collectAsState()
    
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Overview", "Location", "Notifications", "Control")
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(device?.getDisplayName() ?: "Device Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Status banner
            device?.let { dev ->
                StatusBanner(device = dev)
            }
            
            // Command status
            when (val status = commandStatus) {
                is CommandStatus.Loading -> {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                is CommandStatus.Success -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "Command sent successfully",
                            modifier = Modifier.padding(8.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                is CommandStatus.Error -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = "Error: ${status.message}",
                            modifier = Modifier.padding(8.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                else -> {}
            }
            
            // Tabs
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }
            
            // Tab content
            when (selectedTab) {
                0 -> OverviewTab(device = device)
                1 -> LocationTab(
                    viewModel = viewModel,
                    onOpenFullMap = {
                        device?.let { dev ->
                            onNavigateToLocation(deviceId, dev.getDisplayName())
                        }
                    }
                )
                2 -> NotificationsTab(viewModel = viewModel)
                3 -> ControlTab(
                    viewModel = viewModel,
                    onOpenCameraStream = {
                        device?.let { dev ->
                            onNavigateToStream(deviceId, dev.getDisplayName(), "camera")
                        }
                    },
                    onOpenScreenStream = {
                        device?.let { dev ->
                            onNavigateToStream(deviceId, dev.getDisplayName(), "screen")
                        }
                    },
                    onOpenAudioStream = {
                        device?.let { dev ->
                            onNavigateToStream(deviceId, dev.getDisplayName(), "audio")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun StatusBanner(device: com.myparentalcontrol.parent.data.model.ChildDevice) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (device.isOnline) 
            MaterialTheme.colorScheme.primaryContainer 
        else 
            MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (device.isOnline) Icons.Default.CheckCircle else Icons.Default.Cancel,
                    contentDescription = null,
                    tint = if (device.isOnline) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = device.getStatusText(),
                    fontWeight = FontWeight.Medium
                )
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Battery
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when {
                            device.batteryLevel > 80 -> Icons.Default.BatteryFull
                            device.batteryLevel > 20 -> Icons.Default.Battery4Bar
                            else -> Icons.Default.BatteryAlert
                        },
                        contentDescription = "Battery",
                        modifier = Modifier.size(20.dp)
                    )
                    Text("${device.batteryLevel}%", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun OverviewTab(device: com.myparentalcontrol.parent.data.model.ChildDevice?) {
    if (device == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            InfoCard(
                title = "Device Information",
                items = listOf(
                    "Name" to device.deviceName,
                    "Model" to device.deviceModel,
                    "Status" to (if (device.isOnline) "Online" else "Offline"),
                    "Battery" to "${device.batteryLevel}%${if (device.isCharging) " (Charging)" else ""}",
                    "Network" to device.networkType.ifEmpty { "Unknown" }
                )
            )
        }
        
        item {
            InfoCard(
                title = "Features",
                items = listOf(
                    "Notification Access" to if (device.notificationAccessEnabled) "Enabled" else "Disabled",
                    "Location Tracking" to "Active",
                    "Live Streaming" to "Available"
                )
            )
        }
    }
}

@Composable
private fun LocationTab(
    viewModel: DeviceDetailsViewModel,
    onOpenFullMap: () -> Unit = {}
) {
    val locationHistory by viewModel.locationHistory.collectAsState()
    val currentLocation = viewModel.getCurrentLocation()
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Map
        if (currentLocation != null) {
            val cameraPositionState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(currentLocation, 15f)
            }
            
            Box(modifier = Modifier.fillMaxWidth()) {
                GoogleMap(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    cameraPositionState = cameraPositionState
                ) {
                    Marker(
                        state = MarkerState(position = currentLocation),
                        title = "Current Location"
                    )
                }
                
                // Open Full Map Button
                FloatingActionButton(
                    onClick = onOpenFullMap,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Fullscreen, contentDescription = "Full Map")
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text("No location data available")
            }
        }
        
        // Location history list
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Location History",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            IconButton(onClick = { viewModel.refreshLocationHistory() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }
        }
        
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(locationHistory) { location ->
                LocationHistoryItem(location)
            }
        }
    }
}

@Composable
private fun LocationHistoryItem(location: com.myparentalcontrol.parent.data.model.LocationHistory) {
    Card {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                        .format(Date(location.timestamp)),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${location.latitude}, ${location.longitude}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Text(
                text = "±${location.accuracy.toInt()}m",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NotificationsTab(viewModel: DeviceDetailsViewModel) {
    val notifications by viewModel.notifications.collectAsState()
    
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${notifications.size} Notifications",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            IconButton(onClick = { viewModel.refreshNotifications() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }
        }
        
        if (notifications.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No notifications captured")
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(notifications) { notification ->
                    NotificationItem(notification)
                }
            }
        }
    }
}

@Composable
private fun NotificationItem(notification: com.myparentalcontrol.parent.data.model.NotificationData) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = notification.appName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = SimpleDateFormat("HH:mm", Locale.getDefault())
                        .format(Date(notification.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (notification.title.isNotEmpty()) {
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            if (notification.text.isNotEmpty()) {
                Text(
                    text = notification.text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun ControlTab(
    viewModel: DeviceDetailsViewModel,
    onOpenCameraStream: () -> Unit = {},
    onOpenScreenStream: () -> Unit = {},
    onOpenAudioStream: () -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ControlSection(
                title = "Camera Stream",
                description = "View live camera feed from device",
                icon = Icons.Default.Videocam,
                onStart = { viewModel.startCameraStream() },
                onStop = { viewModel.stopCameraStream() },
                onOpenFullScreen = onOpenCameraStream
            )
        }
        
        item {
            ControlSection(
                title = "Screen Mirror",
                description = "View device screen in real-time",
                icon = Icons.Default.ScreenShare,
                onStart = { viewModel.startScreenMirror() },
                onStop = { viewModel.stopScreenMirror() },
                onOpenFullScreen = onOpenScreenStream
            )
        }
        
        item {
            ControlSection(
                title = "Audio Stream",
                description = "Listen to device microphone",
                icon = Icons.Default.Mic,
                onStart = { viewModel.startAudioStream() },
                onStop = { viewModel.stopAudioStream() },
                onOpenFullScreen = onOpenAudioStream
            )
        }
        
        item {
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Quick Actions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Button(
                        onClick = { viewModel.requestLocationUpdate() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Update Location Now")
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedButton(
                        onClick = { viewModel.ringDevice() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ring Device")
                    }
                }
            }
        }
    }
}

@Composable
private fun ControlSection(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onOpenFullScreen: () -> Unit = {}
) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onStart,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Start")
                }
                
                OutlinedButton(
                    onClick = onStop,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Stop")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            FilledTonalButton(
                onClick = onOpenFullScreen,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Fullscreen, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open Full Screen")
            }
        }
    }
}

@Composable
private fun InfoCard(title: String, items: List<Pair<String, String>>) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            items.forEach { (label, value) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
