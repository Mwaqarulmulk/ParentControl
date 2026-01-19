package com.myparentalcontrol.parent.presentation.screens.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Main dashboard screen for parent app
 * Shows list of connected children and quick actions
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToChild: (String, String) -> Unit,
    onNavigateToStream: (String, String) -> Unit,
    onNavigateToAddChild: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    // Mock data - replace with actual data from ViewModel
    val children = remember {
        listOf(
            ChildDevice(
                id = "child_device_1",
                name = "John's Phone",
                isOnline = true,
                batteryLevel = 75,
                lastSeen = "Just now"
            ),
            ChildDevice(
                id = "child_device_2",
                name = "Sarah's Tablet",
                isOnline = false,
                batteryLevel = 45,
                lastSeen = "2 hours ago"
            )
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Parental Control") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToAddChild) {
                Icon(Icons.Default.Add, contentDescription = "Add Child")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Your Children",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            
            if (children.isEmpty()) {
                item {
                    EmptyChildrenCard(onAddChild = onNavigateToAddChild)
                }
            } else {
                items(children) { child ->
                    ChildCard(
                        child = child,
                        onViewDetails = { onNavigateToChild(child.id, child.name) },
                        onStartStream = { onNavigateToStream(child.id, child.name) }
                    )
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(80.dp)) // Space for FAB
            }
        }
    }
}

@Composable
private fun ChildCard(
    child: ChildDevice,
    onViewDetails: () -> Unit,
    onStartStream: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onViewDetails
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Online indicator
                    Icon(
                        imageVector = Icons.Default.Circle,
                        contentDescription = if (child.isOnline) "Online" else "Offline",
                        tint = if (child.isOnline) Color.Green else Color.Gray,
                        modifier = Modifier.size(12.dp)
                    )
                    
                    Column {
                        Text(
                            text = child.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (child.isOnline) "Online" else "Last seen: ${child.lastSeen}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Battery indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = when {
                            child.batteryLevel > 80 -> Icons.Default.BatteryFull
                            child.batteryLevel > 50 -> Icons.Default.Battery5Bar
                            child.batteryLevel > 20 -> Icons.Default.Battery3Bar
                            else -> Icons.Default.Battery1Bar
                        },
                        contentDescription = "Battery",
                        tint = when {
                            child.batteryLevel > 50 -> Color.Green
                            child.batteryLevel > 20 -> Color.Yellow
                            else -> Color.Red
                        },
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "${child.batteryLevel}%",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Quick actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Stream button
                FilledTonalButton(
                    onClick = onStartStream,
                    enabled = child.isOnline,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Stream")
                }
                
                // Location button
                OutlinedButton(
                    onClick = { /* TODO: Navigate to location */ },
                    enabled = child.isOnline,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Location")
                }
                
                // Apps button
                OutlinedButton(
                    onClick = { /* TODO: Navigate to apps */ },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Apps,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Apps")
                }
            }
        }
    }
}

@Composable
private fun EmptyChildrenCard(onAddChild: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FamilyRestroom,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = "No Children Added",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "Add your child's device to start monitoring",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Button(onClick = onAddChild) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Child Device")
            }
        }
    }
}

/**
 * Data class representing a child device
 */
data class ChildDevice(
    val id: String,
    val name: String,
    val isOnline: Boolean,
    val batteryLevel: Int,
    val lastSeen: String
)
