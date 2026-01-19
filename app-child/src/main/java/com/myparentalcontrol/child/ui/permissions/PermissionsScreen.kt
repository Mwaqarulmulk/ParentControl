package com.myparentalcontrol.child.ui.permissions

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.myparentalcontrol.child.util.PermissionManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsScreen(
    onAllPermissionsGranted: () -> Unit,
    viewModel: PermissionsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val permissionsList by viewModel.permissionsList.collectAsState()
    val currentStep by viewModel.currentStep.collectAsState()
    
    // Permission launcher for multiple permissions
    val multiplePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Refresh after permissions are granted
        viewModel.refreshPermissionStatus(context)
    }
    
    // Background location needs separate request
    val backgroundLocationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.refreshPermissionStatus(context)
    }
    
    // Check permission status on resume
    LaunchedEffect(Unit) {
        viewModel.refreshPermissionStatus(context)
    }
    
    // Navigate when all permissions granted
    LaunchedEffect(uiState) {
        if (uiState is PermissionsUiState.AllGranted) {
            kotlinx.coroutines.delay(500)
            onAllPermissionsGranted()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Setup Permissions") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                is PermissionsUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                
                is PermissionsUiState.NeedPermissions -> {
                    RuntimePermissionsContent(
                        permissionsList = permissionsList,
                        grantedCount = state.grantedCount,
                        totalCount = state.totalCount,
                        onRequestPermissions = {
                            val missing = viewModel.getMissingPermissions()
                            if (missing.isNotEmpty()) {
                                multiplePermissionLauncher.launch(missing)
                            }
                        },
                        onRequestBackgroundLocation = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                backgroundLocationLauncher.launch(
                                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                                )
                            }
                        },
                        onOpenSettings = {
                            viewModel.permissionManager.openAppSettings(context)
                        }
                    )
                }
                
                is PermissionsUiState.NeedNotificationListener -> {
                    NotificationListenerContent(
                        onOpenSettings = {
                            viewModel.permissionManager.openNotificationListenerSettings(context)
                        },
                        onRefresh = {
                            viewModel.refreshPermissionStatus(context)
                        }
                    )
                }
                
                is PermissionsUiState.NeedBatteryOptimization -> {
                    BatteryOptimizationContent(
                        onDisable = {
                            viewModel.permissionManager.openBatteryOptimizationSettings(context)
                        },
                        onSkip = {
                            viewModel.skipBatteryOptimization()
                        },
                        onRefresh = {
                            viewModel.refreshPermissionStatus(context)
                        }
                    )
                }
                
                is PermissionsUiState.AllGranted -> {
                    AllGrantedContent()
                }
            }
        }
    }
}

@Composable
private fun RuntimePermissionsContent(
    permissionsList: List<PermissionManager.PermissionInfo>,
    grantedCount: Int,
    totalCount: Int,
    onRequestPermissions: () -> Unit,
    onRequestBackgroundLocation: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Security,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Permissions Required",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "This app needs the following permissions to monitor and protect this device",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        LinearProgressIndicator(
            progress = { grantedCount.toFloat() / totalCount.toFloat() },
            modifier = Modifier.fillMaxWidth(),
        )
        
        Text(
            text = "$grantedCount of $totalCount permissions granted",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(permissionsList) { permission ->
                PermissionItem(permission)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = onRequestPermissions,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Icon(Icons.Default.Check, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Grant Permissions")
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Check if we need background location separately
        val needsBackgroundLocation = permissionsList.any { 
            it.permission == Manifest.permission.ACCESS_BACKGROUND_LOCATION && !it.isGranted 
        }
        val foregroundLocationGranted = permissionsList.any {
            it.permission == Manifest.permission.ACCESS_FINE_LOCATION && it.isGranted
        }
        
        if (needsBackgroundLocation && foregroundLocationGranted) {
            OutlinedButton(
                onClick = onRequestBackgroundLocation,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Grant Background Location")
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        TextButton(
            onClick = onOpenSettings,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Open App Settings")
        }
    }
}

@Composable
private fun PermissionItem(permission: PermissionManager.PermissionInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (permission.isGranted) 
                MaterialTheme.colorScheme.secondaryContainer 
            else 
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
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
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (permission.isGranted)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.error
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getPermissionIcon(permission.name),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = permission.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = permission.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                imageVector = if (permission.isGranted) 
                    Icons.Default.CheckCircle 
                else 
                    Icons.Default.Cancel,
                contentDescription = null,
                tint = if (permission.isGranted) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun NotificationListenerContent(
    onOpenSettings: () -> Unit,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Notifications,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Notification Access",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Enable notification access to allow parents to monitor app notifications on this device.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Instructions:",
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("1. Tap 'Open Settings' below")
                Text("2. Find 'Parental Control Child' in the list")
                Text("3. Toggle it ON")
                Text("4. Return to this app")
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onOpenSettings,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Icon(Icons.Default.Settings, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Open Settings")
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        OutlinedButton(
            onClick = onRefresh,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("I've Enabled It - Check Again")
        }
    }
}

@Composable
private fun BatteryOptimizationContent(
    onDisable: () -> Unit,
    onSkip: () -> Unit,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.BatteryChargingFull,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Battery Optimization",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Disable battery optimization to keep monitoring services running in the background.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onDisable,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Disable Battery Optimization")
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        OutlinedButton(
            onClick = onRefresh,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("I've Done It - Check Again")
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        TextButton(
            onClick = onSkip,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Skip for Now")
        }
    }
}

@Composable
private fun AllGrantedContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "All Set!",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "All permissions have been granted. Proceeding to pairing...",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        CircularProgressIndicator()
    }
}

private fun getPermissionIcon(name: String): ImageVector {
    return when {
        name.contains("Camera", ignoreCase = true) -> Icons.Default.CameraAlt
        name.contains("Micro", ignoreCase = true) -> Icons.Default.Mic
        name.contains("Location", ignoreCase = true) -> Icons.Default.LocationOn
        name.contains("Notif", ignoreCase = true) -> Icons.Default.Notifications
        else -> Icons.Default.Security
    }
}
