package com.myparentalcontrol.child.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.myparentalcontrol.child.ui.home.HomeScreen
import com.myparentalcontrol.child.ui.pairing.ChildPairingScreen
import com.myparentalcontrol.child.ui.pairing.ChildPairingViewModel
import com.myparentalcontrol.child.ui.permissions.PermissionsScreen
import com.myparentalcontrol.child.ui.permissions.PermissionsViewModel

sealed class ChildScreen(val route: String) {
    object Splash : ChildScreen("splash")
    object Permissions : ChildScreen("permissions")
    object Pairing : ChildScreen("pairing")
    object Home : ChildScreen("home")
}

@Composable
fun ChildNavHost() {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = ChildScreen.Splash.route
    ) {
        // Splash screen to check pairing status and permissions
        composable(ChildScreen.Splash.route) {
            SplashScreen(
                onPaired = {
                    navController.navigate(ChildScreen.Home.route) {
                        popUpTo(ChildScreen.Splash.route) { inclusive = true }
                    }
                },
                onNeedPermissions = {
                    navController.navigate(ChildScreen.Permissions.route) {
                        popUpTo(ChildScreen.Splash.route) { inclusive = true }
                    }
                },
                onNeedPairing = {
                    navController.navigate(ChildScreen.Pairing.route) {
                        popUpTo(ChildScreen.Splash.route) { inclusive = true }
                    }
                }
            )
        }
        
        // Permissions screen
        composable(ChildScreen.Permissions.route) {
            PermissionsScreen(
                onAllPermissionsGranted = {
                    navController.navigate(ChildScreen.Pairing.route) {
                        popUpTo(ChildScreen.Permissions.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(ChildScreen.Pairing.route) {
            ChildPairingScreen(
                onPairingComplete = {
                    navController.navigate(ChildScreen.Home.route) {
                        popUpTo(ChildScreen.Pairing.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(ChildScreen.Home.route) {
            HomeScreen()
        }
    }
}

@Composable
private fun SplashScreen(
    onPaired: () -> Unit,
    onNeedPermissions: () -> Unit,
    onNeedPairing: () -> Unit,
    pairingViewModel: ChildPairingViewModel = hiltViewModel(),
    permissionsViewModel: PermissionsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var hasChecked by remember { mutableStateOf(false) }
    
    // Trigger pairing status check
    LaunchedEffect(Unit) {
        pairingViewModel.checkPairingStatus()
        // Give it time to complete
        kotlinx.coroutines.delay(1000)
        hasChecked = true
    }
    
    LaunchedEffect(hasChecked) {
        if (hasChecked) {
            // Re-check the isPaired value after check completes
            val paired = pairingViewModel.isPaired.value
            if (paired) {
                onPaired()
            } else {
                // Not paired - check if permissions are granted
                val permissionsGranted = permissionsViewModel.areAllPermissionsGranted(context)
                if (permissionsGranted) {
                    onNeedPairing()
                } else {
                    onNeedPermissions()
                }
            }
        }
    }
    
    // Show loading while checking
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}
