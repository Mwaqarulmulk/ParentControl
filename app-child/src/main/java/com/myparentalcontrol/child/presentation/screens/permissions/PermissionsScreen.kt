package com.myparentalcontrol.child.presentation.screens.permissions

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.myparentalcontrol.child.R

data class PermissionItem(
    val name: String,
    val description: String,
    val icon: ImageVector,
    val isGranted: Boolean,
    val onRequest: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsScreen(
    onPermissionsGranted: () -> Unit
) {
    val context = LocalContext.current
    var permissionsState by remember { mutableStateOf(checkAllPermissions(context)) }
    
    // Permission launchers
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissionsState = checkAllPermissions(context)
    }
    
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionsState = checkAllPermissions(context)
    }
    
    val microphonePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionsState = checkAllPermissions(context)
    }
    
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionsState = checkAllPermissions(context)
    }
    
    val phonePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissionsState = checkAllPermissions(context)
    }
    
    val smsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionsState = checkAllPermissions(context)
    }
    
    // Check if all required permissions are granted
    LaunchedEffect(permissionsState) {
        if (permissionsState.allGranted) {
            onPermissionsGranted()
        }
    }
    
    val permissionItems = listOf(
        PermissionItem(
            name = "Location",
            description = stringResource(R.string.permission_location_rationale),
            icon = Icons.Default.LocationOn,
            isGranted = permissionsState.locationGranted,
            onRequest = {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    )
                )
            }
        ),
        PermissionItem(
            name = "Camera",
            description = stringResource(R.string.permission_camera_rationale),
            icon = Icons.Default.CameraAlt,
            isGranted = permissionsState.cameraGranted,
            onRequest = {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        ),
        PermissionItem(
            name = "Microphone",
            description = stringResource(R.string.permission_microphone_rationale),
            icon = Icons.Default.Mic,
            isGranted = permissionsState.microphoneGranted,
            onRequest = {
                microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        ),
        PermissionItem(
            name = "Usage Access",
            description = stringResource(R.string.permission_usage_stats_rationale),
            icon = Icons.Default.BarChart,
            isGranted = permissionsState.usageStatsGranted,
            onRequest = {
                context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            }
        ),
        PermissionItem(
            name = "Overlay Permission",
            description = stringResource(R.string.permission_overlay_rationale),
            icon = Icons.Default.Layers,
            isGranted = permissionsState.overlayGranted,
            onRequest = {
                context.startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                )
            }
        ),
        PermissionItem(
            name = "Notifications",
            description = stringResource(R.string.permission_notification_rationale),
            icon = Icons.Default.Notifications,
            isGranted = permissionsState.notificationGranted,
            onRequest = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        ),
        PermissionItem(
            name = "Phone Calls",
            description = "Access to call logs for monitoring",
            icon = Icons.Default.Phone,
            isGranted = permissionsState.phoneGranted,
            onRequest = {
                phonePermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.READ_CALL_LOG,
                        Manifest.permission.READ_PHONE_STATE
                    )
                )
            }
        ),
        PermissionItem(
            name = "SMS",
            description = "Access to SMS messages for monitoring",
            icon = Icons.Default.Sms,
            isGranted = permissionsState.smsGranted,
            onRequest = {
                smsPermissionLauncher.launch(Manifest.permission.READ_SMS)
            }
        )
    )
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Permissions Required") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LinearProgressIndicator(
                progress = { permissionsState.grantedCount.toFloat() / permissionItems.size },
                modifier = Modifier.fillMaxWidth(),
            )
            
            Text(
                text = "${permissionsState.grantedCount} of ${permissionItems.size} permissions granted",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(permissionItems) { permission ->
                    PermissionCard(permission = permission)
                }
            }
        }
    }
}

@Composable
fun PermissionCard(permission: PermissionItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (permission.isGranted) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = permission.icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = if (permission.isGranted) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = permission.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = permission.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            if (permission.isGranted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Granted",
                    tint = MaterialTheme.colorScheme.primary
                )
            } else {
                Button(
                    onClick = permission.onRequest,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text("Grant")
                }
            }
        }
    }
}

data class PermissionsState(
    val locationGranted: Boolean = false,
    val cameraGranted: Boolean = false,
    val microphoneGranted: Boolean = false,
    val usageStatsGranted: Boolean = false,
    val overlayGranted: Boolean = false,
    val notificationGranted: Boolean = false,
    val phoneGranted: Boolean = false,
    val smsGranted: Boolean = false
) {
    val grantedCount: Int
        get() = listOf(
            locationGranted, cameraGranted, microphoneGranted,
            usageStatsGranted, overlayGranted, notificationGranted,
            phoneGranted, smsGranted
        ).count { it }
    
    val allGranted: Boolean
        get() = locationGranted && cameraGranted && microphoneGranted &&
                usageStatsGranted && overlayGranted && notificationGranted &&
                phoneGranted && smsGranted
}

private fun checkAllPermissions(context: Context): PermissionsState {
    return PermissionsState(
        locationGranted = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == 
            android.content.pm.PackageManager.PERMISSION_GRANTED,
        cameraGranted = context.checkSelfPermission(Manifest.permission.CAMERA) == 
            android.content.pm.PackageManager.PERMISSION_GRANTED,
        microphoneGranted = context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == 
            android.content.pm.PackageManager.PERMISSION_GRANTED,
        usageStatsGranted = hasUsageStatsPermission(context),
        overlayGranted = Settings.canDrawOverlays(context),
        notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == 
                android.content.pm.PackageManager.PERMISSION_GRANTED
        } else true,
        phoneGranted = context.checkSelfPermission(Manifest.permission.READ_CALL_LOG) == 
            android.content.pm.PackageManager.PERMISSION_GRANTED,
        smsGranted = context.checkSelfPermission(Manifest.permission.READ_SMS) == 
            android.content.pm.PackageManager.PERMISSION_GRANTED
    )
}

private fun hasUsageStatsPermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
    val mode = appOps.checkOpNoThrow(
        android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
        android.os.Process.myUid(),
        context.packageName
    )
    return mode == android.app.AppOpsManager.MODE_ALLOWED
}
