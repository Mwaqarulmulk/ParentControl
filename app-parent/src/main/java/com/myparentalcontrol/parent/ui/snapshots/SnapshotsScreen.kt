package com.myparentalcontrol.parent.ui.snapshots

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * Screen to view snapshots captured from child device
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnapshotsScreen(
    deviceId: String,
    deviceName: String,
    onNavigateBack: () -> Unit,
    viewModel: SnapshotsViewModel = hiltViewModel()
) {
    val snapshots by viewModel.snapshots.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var selectedSnapshot by remember { mutableStateOf<SnapshotData?>(null) }

    LaunchedEffect(deviceId) {
        viewModel.loadSnapshots(deviceId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Snapshots - $deviceName") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadSnapshots(deviceId) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        floatingActionButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FloatingActionButton(
                    onClick = { viewModel.requestCameraSnapshot(deviceId) },
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Camera Snapshot")
                }
                FloatingActionButton(
                    onClick = { viewModel.requestScreenSnapshot(deviceId) },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(Icons.Default.Screenshot, contentDescription = "Screen Snapshot")
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                snapshots.isEmpty() -> {
                    EmptySnapshotsView(
                        onTakeCameraSnapshot = { viewModel.requestCameraSnapshot(deviceId) },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(snapshots) { snapshot ->
                            SnapshotItem(
                                snapshot = snapshot,
                                onClick = { selectedSnapshot = snapshot }
                            )
                        }
                    }
                }
            }
        }
    }

    // Full screen image dialog
    selectedSnapshot?.let { snapshot ->
        Dialog(onDismissRequest = { selectedSnapshot = null }) {
            FullScreenSnapshotView(
                snapshot = snapshot,
                onDismiss = { selectedSnapshot = null },
                onDelete = {
                    viewModel.deleteSnapshot(deviceId, snapshot.id)
                    selectedSnapshot = null
                }
            )
        }
    }
}

@Composable
private fun EmptySnapshotsView(
    onTakeCameraSnapshot: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CameraAlt,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
        Text(
            text = "No Snapshots Yet",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Capture camera or screen snapshots from the child device",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Button(onClick = onTakeCameraSnapshot) {
            Icon(Icons.Default.CameraAlt, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Take Camera Snapshot")
        }
    }
}

@Composable
private fun SnapshotItem(
    snapshot: SnapshotData,
    onClick: () -> Unit
) {
    // Decode Base64 image if available
    val bitmap = remember(snapshot.imageData) {
        if (snapshot.imageData.isNotEmpty()) {
            try {
                val bytes = Base64.decode(snapshot.imageData, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } catch (e: Exception) {
                null
            }
        } else null
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                bitmap != null -> {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Snapshot",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                snapshot.status == "pending" -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                snapshot.status == "requires_permission" -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFFF9800)
                            )
                            Text(
                                text = "Permission Required",
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.BrokenImage,
                            contentDescription = null,
                            tint = Color.Gray
                        )
                    }
                }
            }

            // Type badge
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp),
                color = if (snapshot.type == "camera") Color(0xFF4CAF50) else Color(0xFF2196F3),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = if (snapshot.type == "camera") "CAM" else "SCR",
                    color = Color.White,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            // Timestamp
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(),
                color = Color.Black.copy(alpha = 0.6f)
            ) {
                Text(
                    text = formatTimestamp(snapshot.timestamp),
                    color = Color.White,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}

@Composable
private fun FullScreenSnapshotView(
    snapshot: SnapshotData,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    // Decode Base64 image if available
    val bitmap = remember(snapshot.imageData) {
        if (snapshot.imageData.isNotEmpty()) {
            try {
                val bytes = Base64.decode(snapshot.imageData, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } catch (e: Exception) {
                null
            }
        } else null
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.8f),
        shape = RoundedCornerShape(16.dp)
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
                Column {
                    Text(
                        text = if (snapshot.type == "camera") "Camera Snapshot" else "Screen Snapshot",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = formatTimestamp(snapshot.timestamp),
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
                Row {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            }

            // Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                when {
                    bitmap != null -> {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Full Snapshot",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                    snapshot.status == "pending" -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = Color.White)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Loading snapshot...",
                                color = Color.White
                            )
                        }
                    }
                    else -> {
                        Text(
                            text = snapshot.message ?: "Image not available",
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    return SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(timestamp))
}

/**
 * Data class for snapshot
 */
data class SnapshotData(
    val id: String = "",
    val type: String = "",
    val url: String = "",
    val imageData: String = "", // Base64 encoded image data
    val status: String = "",
    val timestamp: Long = 0L,
    val message: String? = null
) {
    /**
     * Check if this snapshot has displayable image data
     */
    fun hasImage(): Boolean = imageData.isNotEmpty() || url.isNotEmpty()
    
    /**
     * Get the image source - either URL or Base64 data URI
     */
    fun getImageSource(): String? {
        return when {
            imageData.isNotEmpty() -> "data:image/jpeg;base64,$imageData"
            url.isNotEmpty() -> url
            else -> null
        }
    }
}
