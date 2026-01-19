package com.myparentalcontrol.parent.ui.location

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Live Location Screen with real-time map tracking
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveLocationScreen(
    deviceId: String,
    deviceName: String,
    onNavigateBack: () -> Unit,
    viewModel: LiveLocationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val geofences by viewModel.geofences.collectAsState()
    val locationHistory by viewModel.locationHistory.collectAsState()
    
    var showGeofenceDialog by remember { mutableStateOf(false) }
    var showHistoryPanel by remember { mutableStateOf(false) }
    
    // Initialize with device ID
    LaunchedEffect(deviceId) {
        viewModel.initialize(deviceId)
    }
    
    // Camera position state
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            uiState.currentLocation ?: LatLng(0.0, 0.0),
            15f
        )
    }
    
    // Update camera when location changes
    LaunchedEffect(uiState.currentLocation) {
        uiState.currentLocation?.let { location ->
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(location, 16f),
                durationMs = 1000
            )
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(deviceName)
                        Text(
                            text = if (uiState.isOnline) "Online • Live tracking" else "Offline",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (uiState.isOnline) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Refresh location
                    IconButton(onClick = { viewModel.requestLocationUpdate() }) {
                        Icon(Icons.Default.MyLocation, contentDescription = "Update Location")
                    }
                    // Toggle history panel
                    IconButton(onClick = { showHistoryPanel = !showHistoryPanel }) {
                        Icon(Icons.Default.History, contentDescription = "Location History")
                    }
                    // Add geofence
                    IconButton(onClick = { showGeofenceDialog = true }) {
                        Icon(Icons.Default.AddLocation, contentDescription = "Add Geofence")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Google Map
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = true,
                    myLocationButtonEnabled = false
                )
            ) {
                // Current location marker
                uiState.currentLocation?.let { location ->
                    Marker(
                        state = MarkerState(position = location),
                        title = deviceName,
                        snippet = "Last updated: ${uiState.lastUpdateTime ?: "Unknown"}"
                    )
                    
                    // Accuracy circle
                    uiState.accuracy?.let { accuracy ->
                        Circle(
                            center = location,
                            radius = accuracy.toDouble(),
                            strokeColor = Color(0x440000FF),
                            fillColor = Color(0x220000FF),
                            strokeWidth = 2f
                        )
                    }
                }
                
                // Geofence circles
                geofences.forEach { geofence ->
                    Circle(
                        center = LatLng(geofence.latitude, geofence.longitude),
                        radius = geofence.radius.toDouble(),
                        strokeColor = if (geofence.isInside) Color(0xFF4CAF50) else Color(0xFFFF9800),
                        fillColor = if (geofence.isInside) Color(0x224CAF50) else Color(0x22FF9800),
                        strokeWidth = 3f
                    )
                    
                    Marker(
                        state = MarkerState(position = LatLng(geofence.latitude, geofence.longitude)),
                        title = geofence.name,
                        snippet = if (geofence.isInside) "Inside zone" else "Outside zone"
                    )
                }
                
                // Location history polyline
                if (locationHistory.isNotEmpty()) {
                    Polyline(
                        points = locationHistory.map { LatLng(it.latitude, it.longitude) },
                        color = Color(0xFF2196F3),
                        width = 5f
                    )
                }
            }
            
            // Status card at bottom
            LocationStatusCard(
                uiState = uiState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            )
            
            // History panel (slide in from right)
            if (showHistoryPanel) {
                LocationHistoryPanel(
                    history = locationHistory,
                    onClose = { showHistoryPanel = false },
                    onLocationClick = { location ->
                        cameraPositionState.move(
                            CameraUpdateFactory.newLatLng(LatLng(location.latitude, location.longitude))
                        )
                    },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .width(300.dp)
                )
            }
            
            // Loading indicator
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
    
    // Geofence dialog
    if (showGeofenceDialog) {
        AddGeofenceDialog(
            currentLocation = uiState.currentLocation,
            onDismiss = { showGeofenceDialog = false },
            onConfirm = { name, lat, lng, radius ->
                viewModel.addGeofence(name, lat, lng, radius)
                showGeofenceDialog = false
            }
        )
    }
}

@Composable
private fun LocationStatusCard(
    uiState: LiveLocationUiState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Status indicator
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(if (uiState.isOnline) Color(0xFF4CAF50) else Color.Gray)
                    )
                    Text(
                        text = if (uiState.isOnline) "Live" else "Last Known",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (uiState.isOnline) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Battery
                uiState.batteryLevel?.let { battery ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when {
                                battery > 80 -> Icons.Default.BatteryFull
                                battery > 50 -> Icons.Default.Battery5Bar
                                battery > 20 -> Icons.Default.Battery3Bar
                                else -> Icons.Default.BatteryAlert
                            },
                            contentDescription = "Battery",
                            modifier = Modifier.size(16.dp),
                            tint = if (battery <= 20) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$battery%",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Address or coordinates
            Text(
                text = uiState.address ?: uiState.currentLocation?.let { 
                    "%.6f, %.6f".format(it.latitude, it.longitude) 
                } ?: "Location unavailable",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            
            // Last update time
            uiState.lastUpdateTime?.let { time ->
                Text(
                    text = "Updated: $time",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Accuracy
            uiState.accuracy?.let { accuracy ->
                Text(
                    text = "Accuracy: ±${accuracy.toInt()}m",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LocationHistoryPanel(
    history: List<LocationHistoryItem>,
    onClose: () -> Unit,
    onLocationClick: (LocationHistoryItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
    ) {
        Column {
            // Header
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
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
            
            Divider()
            
            // History list
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(history) { location ->
                    LocationHistoryRow(
                        location = location,
                        onClick = { onLocationClick(location) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocationHistoryRow(
    location: LocationHistoryItem,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = location.formattedTime,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "%.5f, %.5f".format(location.latitude, location.longitude),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AddGeofenceDialog(
    currentLocation: LatLng?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, lat: Double, lng: Double, radius: Float) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var radius by remember { mutableStateOf(100f) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Safe Zone") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Zone Name") },
                    placeholder = { Text("e.g., School, Home") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text("Radius: ${radius.toInt()}m")
                Slider(
                    value = radius,
                    onValueChange = { radius = it },
                    valueRange = 50f..500f,
                    steps = 8
                )
                
                Text(
                    text = "Zone will be created at current location",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    currentLocation?.let { loc ->
                        onConfirm(name, loc.latitude, loc.longitude, radius)
                    }
                },
                enabled = name.isNotBlank() && currentLocation != null
            ) {
                Text("Add Zone")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Data classes
data class LiveLocationUiState(
    val currentLocation: LatLng? = null,
    val accuracy: Float? = null,
    val address: String? = null,
    val lastUpdateTime: String? = null,
    val batteryLevel: Int? = null,
    val isOnline: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

data class GeofenceItem(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radius: Float,
    val isInside: Boolean = false
)

data class LocationHistoryItem(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val accuracy: Float = 0f
) {
    val formattedTime: String
        get() = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(timestamp))
}
