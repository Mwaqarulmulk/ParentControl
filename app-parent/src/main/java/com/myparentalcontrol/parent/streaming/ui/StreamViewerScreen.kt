package com.myparentalcontrol.parent.streaming.ui

import android.view.ViewGroup
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.myparentalcontrol.parent.streaming.viewmodel.StreamingUiState
import com.myparentalcontrol.parent.streaming.viewmodel.StreamingViewModel
import com.myparentalcontrol.shared.streaming.enums.AudioSource
import com.myparentalcontrol.shared.streaming.enums.ConnectionState
import com.myparentalcontrol.shared.streaming.enums.StreamType
import com.myparentalcontrol.shared.streaming.enums.VideoQuality
import org.webrtc.SurfaceViewRenderer

/**
 * Main stream viewer screen for parent app
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamViewerScreen(
    childDeviceId: String,
    childName: String,
    initialStreamType: String = "camera",
    onNavigateBack: () -> Unit,
    viewModel: StreamingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    // Initialize on first composition
    LaunchedEffect(childDeviceId) {
        // Get parent ID from Firebase Auth
        val parentId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "unknown"
        viewModel.initialize(parentId)
        
        // Auto-start stream based on initial type
        when (initialStreamType.lowercase()) {
            "camera" -> viewModel.requestCameraStream(childDeviceId)
            "screen" -> viewModel.requestScreenStream(childDeviceId)
            "audio" -> viewModel.requestAudioStream(childDeviceId)
        }
    }
    
    // Clean up on dispose
    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopStream()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(childName)
                        if (uiState.isStreaming) {
                            Text(
                                text = getStreamTypeLabel(uiState.currentStreamType),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.stopStream()
                        onNavigateBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Connection status indicator
                    ConnectionStatusIndicator(uiState.connectionState)
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    LoadingContent()
                }
                uiState.isConnected && uiState.hasVideo -> {
                    VideoContent(
                        viewModel = viewModel,
                        uiState = uiState
                    )
                }
                uiState.isConnected && uiState.hasAudio && !uiState.hasVideo -> {
                    AudioOnlyContent(
                        uiState = uiState,
                        onToggleMute = { viewModel.toggleAudio() }
                    )
                }
                uiState.connectionState == ConnectionState.DISCONNECTED -> {
                    StreamSelectionContent(
                        childDeviceId = childDeviceId,
                        onRequestCamera = { type, withAudio, quality ->
                            viewModel.requestCameraStream(
                                childDeviceId = childDeviceId,
                                cameraType = type,
                                withAudio = withAudio,
                                videoQuality = quality
                            )
                        },
                        onRequestScreen = { withAudio, audioSource, quality ->
                            viewModel.requestScreenStream(
                                childDeviceId = childDeviceId,
                                withAudio = withAudio,
                                audioSource = audioSource,
                                videoQuality = quality
                            )
                        },
                        onRequestAudio = { audioSource ->
                            viewModel.requestAudioStream(
                                childDeviceId = childDeviceId,
                                audioSource = audioSource
                            )
                        }
                    )
                }
                uiState.error != null -> {
                    ErrorContent(
                        error = uiState.error!!,
                        onRetry = { viewModel.clearError() },
                        onDismiss = { viewModel.clearError() }
                    )
                }
                else -> {
                    ConnectingContent()
                }
            }
            
            // Floating controls when streaming
            if (uiState.isConnected) {
                StreamControls(
                    uiState = uiState,
                    onToggleAudio = { viewModel.toggleAudio() },
                    onToggleVideo = { viewModel.toggleVideo() },
                    onStopStream = { viewModel.stopStream() },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun ConnectionStatusIndicator(state: ConnectionState) {
    val (color, icon) = when (state) {
        ConnectionState.CONNECTED -> Color.Green to Icons.Default.CheckCircle
        ConnectionState.CONNECTING, ConnectionState.RECONNECTING -> Color.Yellow to Icons.Default.Refresh
        ConnectionState.FAILED -> Color.Red to Icons.Default.Error
        else -> Color.Gray to Icons.Default.Circle
    }
    
    Icon(
        imageVector = icon,
        contentDescription = "Connection: ${state.name}",
        tint = color,
        modifier = Modifier.padding(8.dp)
    )
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text("Connecting to device...")
        }
    }
}

@Composable
private fun ConnectingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text("Waiting for stream...")
        }
    }
}

@Composable
private fun VideoContent(
    viewModel: StreamingViewModel,
    uiState: StreamingUiState
) {
    val context = LocalContext.current
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // WebRTC Video View
        AndroidView(
            factory = { ctx ->
                SurfaceViewRenderer(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    viewModel.getWebRTCManager().setupVideoRenderer(this)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // Stream info overlay
        StreamInfoOverlay(
            uiState = uiState,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        )
    }
}

@Composable
private fun AudioOnlyContent(
    uiState: StreamingUiState,
    onToggleMute: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Audio visualization placeholder
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (uiState.isAudioMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(64.dp)
                )
            }
            
            Text(
                text = "Audio Stream Active",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "Duration: ${uiState.streamStatus.getFormattedDuration()}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Mute button
            FilledTonalButton(onClick = onToggleMute) {
                Icon(
                    imageVector = if (uiState.isAudioMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (uiState.isAudioMuted) "Unmute" else "Mute")
            }
        }
    }
}

@Composable
private fun StreamSelectionContent(
    childDeviceId: String,
    onRequestCamera: (StreamType, Boolean, VideoQuality) -> Unit,
    onRequestScreen: (Boolean, AudioSource, VideoQuality) -> Unit,
    onRequestAudio: (AudioSource) -> Unit
) {
    var selectedQuality by remember { mutableStateOf(VideoQuality.MEDIUM) }
    var includeAudio by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Select Stream Type",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        // Quality selector
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Video Quality",
                    style = MaterialTheme.typography.titleMedium
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VideoQuality.entries.forEach { quality ->
                        FilterChip(
                            selected = selectedQuality == quality,
                            onClick = { selectedQuality = quality },
                            label = { Text(quality.name) }
                        )
                    }
                }
                
                // Audio toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = includeAudio,
                        onCheckedChange = { includeAudio = it }
                    )
                    Text("Include Audio")
                }
            }
        }
        
        // Camera options
        Text(
            text = "Camera",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StreamOptionCard(
                title = "Front Camera",
                icon = Icons.Default.CameraFront,
                onClick = { onRequestCamera(StreamType.CAMERA_FRONT, includeAudio, selectedQuality) },
                modifier = Modifier.weight(1f)
            )
            
            StreamOptionCard(
                title = "Back Camera",
                icon = Icons.Default.CameraRear,
                onClick = { onRequestCamera(StreamType.CAMERA_BACK, includeAudio, selectedQuality) },
                modifier = Modifier.weight(1f)
            )
        }
        
        // Screen option
        Text(
            text = "Screen",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
        
        StreamOptionCard(
            title = "Screen Share",
            icon = Icons.Default.ScreenShare,
            description = "View child's screen",
            onClick = { onRequestScreen(includeAudio, AudioSource.DEVICE_AUDIO, selectedQuality) },
            modifier = Modifier.fillMaxWidth()
        )
        
        // Audio only option
        Text(
            text = "Audio Only",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
        
        StreamOptionCard(
            title = "Listen",
            icon = Icons.Default.Mic,
            description = "Audio from microphone",
            onClick = { onRequestAudio(AudioSource.MICROPHONE) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun StreamOptionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null
) {
    Card(
        onClick = onClick,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StreamInfoOverlay(
    uiState: StreamingUiState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = getStreamTypeLabel(uiState.currentStreamType),
                color = Color.White,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = uiState.streamStatus.getFormattedDuration(),
                color = Color.White,
                style = MaterialTheme.typography.bodySmall
            )
            if (uiState.hasAudio) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (uiState.isAudioMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (uiState.isAudioMuted) "Muted" else "Audio On",
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun StreamControls(
    uiState: StreamingUiState,
    onToggleAudio: () -> Unit,
    onToggleVideo: () -> Unit,
    onStopStream: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Audio toggle
            if (uiState.hasAudio) {
                IconButton(onClick = onToggleAudio) {
                    Icon(
                        imageVector = if (uiState.isAudioMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        contentDescription = if (uiState.isAudioMuted) "Unmute" else "Mute",
                        tint = Color.White
                    )
                }
            }
            
            // Video toggle
            if (uiState.hasVideo) {
                IconButton(onClick = onToggleVideo) {
                    Icon(
                        imageVector = if (uiState.isVideoPaused) Icons.Default.VideocamOff else Icons.Default.Videocam,
                        contentDescription = if (uiState.isVideoPaused) "Resume Video" else "Pause Video",
                        tint = Color.White
                    )
                }
            }
            
            // Stop button
            IconButton(
                onClick = onStopStream,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Red
                )
            ) {
                Icon(
                    imageVector = Icons.Default.CallEnd,
                    contentDescription = "Stop Stream",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun ErrorContent(
    error: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )
                
                Text(
                    text = "Connection Error",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Dismiss")
                    }
                    Button(onClick = onRetry) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}

private fun getStreamTypeLabel(type: StreamType?): String {
    return when (type) {
        StreamType.CAMERA_FRONT -> "Front Camera"
        StreamType.CAMERA_BACK -> "Back Camera"
        StreamType.SCREEN -> "Screen Share"
        StreamType.AUDIO_ONLY -> "Audio Only"
        null -> "Not Streaming"
    }
}
