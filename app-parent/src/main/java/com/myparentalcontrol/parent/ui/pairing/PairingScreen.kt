package com.myparentalcontrol.parent.ui.pairing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PairingScreen(
    onNavigateBack: () -> Unit,
    onPairingComplete: (String) -> Unit = {},
    viewModel: PairingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Listen for pairing complete events
    LaunchedEffect(Unit) {
        viewModel.pairingCompleteEvent.collectLatest { deviceId ->
            onPairingComplete(deviceId)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pair Child Device") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            when (val state = uiState) {
                is PairingUiState.Initial -> {
                    InitialView(onGenerateCode = { viewModel.generatePairingCode() })
                }
                is PairingUiState.Loading -> {
                    LoadingView()
                }
                is PairingUiState.CodeGenerated -> {
                    CodeGeneratedView(
                        code = state.code,
                        onRegenerateCode = { viewModel.generatePairingCode() }
                    )
                }
                is PairingUiState.CodeExpired -> {
                    CodeExpiredView(onGenerateNew = { viewModel.generatePairingCode() })
                }
                is PairingUiState.PairingSuccess -> {
                    PairingSuccessView(
                        onDone = { onPairingComplete(state.childDeviceId) },
                        onGoToDashboard = onNavigateBack
                    )
                }
                is PairingUiState.Error -> {
                    ErrorView(
                        message = state.message,
                        onRetry = { viewModel.generatePairingCode() }
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingView() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(64.dp)
        )
        Text(
            text = "Generating Pairing Code...",
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = "Please wait while we create a secure code",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun InitialView(onGenerateCode: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Icon(
            imageVector = Icons.Default.QrCode,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Text(
            text = "Pair a Child Device",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "Generate a pairing code to link your child's device with this parent app",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Button(
            onClick = onGenerateCode,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Generate Pairing Code", fontSize = 16.sp)
        }
    }
}

@Composable
private fun CodeGeneratedView(code: com.myparentalcontrol.parent.data.model.PairingCode, onRegenerateCode: () -> Unit) {
    var timeRemaining by remember { mutableIntStateOf(
        ((code.expiresAt - System.currentTimeMillis()) / 1000).toInt().coerceAtLeast(0)
    ) }
    
    // Update time remaining every second
    LaunchedEffect(code) {
        while (timeRemaining > 0) {
            kotlinx.coroutines.delay(1000)
            timeRemaining = ((code.expiresAt - System.currentTimeMillis()) / 1000).toInt().coerceAtLeast(0)
        }
    }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = "Pairing Code",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        // Large code display
        Box(
            modifier = Modifier
                .background(
                    MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.large
                )
                .padding(32.dp)
        ) {
            Text(
                text = code.code,
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 48.sp,
                letterSpacing = 8.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        
        Text(
            text = "Enter this code on the child device",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        
        // Waiting indicator
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp
            )
            Text(
                text = "Waiting for child device to connect...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        LinearProgressIndicator(
            progress = { timeRemaining / 300f },
            modifier = Modifier.fillMaxWidth()
        )
        
        Text(
            text = "Code expires in ${timeRemaining / 60}:${(timeRemaining % 60).toString().padStart(2, '0')}",
            style = MaterialTheme.typography.bodyMedium,
            color = if (timeRemaining < 60) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedButton(
            onClick = onRegenerateCode,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Generate New Code")
        }
    }
}

@Composable
private fun CodeExpiredView(onGenerateNew: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.error
        )
        
        Text(
            text = "Code Expired",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.error
        )
        
        Text(
            text = "This pairing code has expired. Generate a new one to continue.",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Button(
            onClick = onGenerateNew,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Generate New Code")
        }
    }
}

@Composable
private fun PairingSuccessView(onDone: () -> Unit, onGoToDashboard: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Text(
            text = "Device Paired!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Text(
            text = "The child device has been successfully linked to your account. You can now monitor and manage the device.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("View Device Details", fontSize = 16.sp)
        }
        
        OutlinedButton(
            onClick = onGoToDashboard,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Go to Dashboard", fontSize = 16.sp)
        }
    }
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.error
        )
        
        Text(
            text = "Error",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.error
        )
        
        Text(
            text = message,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Try Again")
        }
    }
}
