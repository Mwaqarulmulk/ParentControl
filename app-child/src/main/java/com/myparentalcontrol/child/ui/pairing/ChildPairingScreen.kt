package com.myparentalcontrol.child.ui.pairing

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildPairingScreen(
    onPairingComplete: () -> Unit,
    viewModel: ChildPairingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var code by remember { mutableStateOf("") }
    
    // Handle navigation when pairing is complete or already paired
    LaunchedEffect(uiState) {
        when (uiState) {
            is ChildPairingUiState.Success -> {
                kotlinx.coroutines.delay(2000)
                onPairingComplete()
            }
            is ChildPairingUiState.AlreadyPaired -> {
                onPairingComplete()
            }
            else -> {}
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pair with Parent") }
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
                is ChildPairingUiState.Initial -> {
                    PairingInputView(
                        code = code,
                        onCodeChange = { code = it },
                        onSubmit = { viewModel.verifyAndPair(code) }
                    )
                }
                is ChildPairingUiState.AlreadyPaired -> {
                    AlreadyPairedView()
                }
                is ChildPairingUiState.Loading -> {
                    LoadingView(message = state.message)
                }
                is ChildPairingUiState.Success -> {
                    SuccessView()
                }
                is ChildPairingUiState.Error -> {
                    ErrorView(
                        message = state.message,
                        onRetry = { 
                            code = ""
                            viewModel.resetState() 
                        },
                        onClearData = {
                            code = ""
                            viewModel.clearPairingData()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PairingInputView(
    code: String,
    onCodeChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Link,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Text(
            text = "Enter Pairing Code",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "Enter the 6-digit code shown on your parent device",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        OutlinedTextField(
            value = code,
            onValueChange = { if (it.length <= 6 && it.all { char -> char.isDigit() }) onCodeChange(it) },
            modifier = Modifier.fillMaxWidth(),
            textStyle = LocalTextStyle.current.copy(
                fontSize = 32.sp,
                letterSpacing = 8.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            ),
            placeholder = { 
                Text(
                    "000000",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                ) 
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        
        Button(
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = code.length == 6
        ) {
            Text("Pair Device", fontSize = 16.sp)
        }
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
        
        Text(
            text = "This device will be monitored by the parent app after pairing",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AlreadyPairedView() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CircularProgressIndicator(modifier = Modifier.size(64.dp))
        Text(
            text = "Device already paired",
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = "Redirecting to home...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LoadingView(message: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CircularProgressIndicator(modifier = Modifier.size(64.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = "Please wait...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SuccessView() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Text(
            text = "Pairing Successful!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Text(
            text = "This device is now connected to the parent app",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        CircularProgressIndicator(modifier = Modifier.size(24.dp))
        
        Text(
            text = "Setting up monitoring services...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit, onClearData: () -> Unit) {
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
            text = "Pairing Failed",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error
        )
        
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Text(
                text = message,
                modifier = Modifier.padding(16.dp),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Try Again")
        }
        
        OutlinedButton(
            onClick = onClearData,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text("Reset Pairing Data")
        }
        
        Text(
            text = "Use 'Reset Pairing Data' if you're having persistent issues",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
