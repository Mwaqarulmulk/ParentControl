package com.myparentalcontrol.parent.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.myparentalcontrol.parent.ui.auth.AuthViewModel
import com.myparentalcontrol.parent.ui.auth.LoginScreen
import com.myparentalcontrol.parent.ui.dashboard.DashboardScreen
import com.myparentalcontrol.parent.ui.device.ChildDeviceScreen
import com.myparentalcontrol.parent.ui.device.DeviceDetailsScreen
import com.myparentalcontrol.parent.ui.location.LiveLocationScreen
import com.myparentalcontrol.parent.ui.pairing.PairingScreen
import com.myparentalcontrol.parent.ui.snapshots.SnapshotsScreen
import com.myparentalcontrol.parent.streaming.ui.StreamViewerScreen

sealed class ParentScreen(val route: String) {
    object Login : ParentScreen("login")
    object Dashboard : ParentScreen("dashboard")
    object Pairing : ParentScreen("pairing")
    object DeviceDetails : ParentScreen("device/{deviceId}") {
        fun createRoute(deviceId: String) = "device/$deviceId"
    }
    object LiveLocation : ParentScreen("location/{deviceId}/{deviceName}") {
        fun createRoute(deviceId: String, deviceName: String) = "location/$deviceId/$deviceName"
    }
    object LiveStream : ParentScreen("stream/{deviceId}/{deviceName}/{streamType}") {
        fun createRoute(deviceId: String, deviceName: String, streamType: String = "camera") = 
            "stream/$deviceId/$deviceName/$streamType"
    }
    object Snapshots : ParentScreen("snapshots/{deviceId}/{deviceName}") {
        fun createRoute(deviceId: String, deviceName: String) = "snapshots/$deviceId/$deviceName"
    }
}

@Composable
fun ParentNavHost(
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val isAuthenticated by authViewModel.isAuthenticated.collectAsState()
    
    // Determine start destination based on auth state
    val startDestination = if (isAuthenticated) {
        ParentScreen.Dashboard.route
    } else {
        ParentScreen.Login.route
    }
    
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Login Screen
        composable(ParentScreen.Login.route) {
            LoginScreen(
                onAuthSuccess = {
                    navController.navigate(ParentScreen.Dashboard.route) {
                        popUpTo(ParentScreen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        
        // Dashboard Screen
        composable(ParentScreen.Dashboard.route) {
            DashboardScreen(
                onNavigateToDevice = { deviceId ->
                    navController.navigate(ParentScreen.DeviceDetails.createRoute(deviceId))
                },
                onNavigateToPairing = {
                    navController.navigate(ParentScreen.Pairing.route)
                },
                onSignOut = {
                    authViewModel.signOut()
                    navController.navigate(ParentScreen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        
        // Pairing Screen
        composable(ParentScreen.Pairing.route) {
            PairingScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onPairingComplete = { deviceId ->
                    // Navigate to device details after successful pairing
                    navController.navigate(ParentScreen.DeviceDetails.createRoute(deviceId)) {
                        popUpTo(ParentScreen.Dashboard.route) { inclusive = false }
                    }
                }
            )
        }
        
        // Device Details Screen (FlashGet Kids style)
        composable(
            route = ParentScreen.DeviceDetails.route,
            arguments = listOf(
                navArgument("deviceId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val deviceId = backStackEntry.arguments?.getString("deviceId") ?: return@composable
            ChildDeviceScreen(
                deviceId = deviceId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToLocation = { id, name ->
                    navController.navigate(ParentScreen.LiveLocation.createRoute(id, name))
                },
                onNavigateToStream = { id, name, streamType ->
                    navController.navigate(ParentScreen.LiveStream.createRoute(id, name, streamType))
                },
                onNavigateToSnapshots = { id, name ->
                    navController.navigate(ParentScreen.Snapshots.createRoute(id, name))
                }
            )
        }
        
        // Live Location Screen
        composable(
            route = ParentScreen.LiveLocation.route,
            arguments = listOf(
                navArgument("deviceId") { type = NavType.StringType },
                navArgument("deviceName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val deviceId = backStackEntry.arguments?.getString("deviceId") ?: return@composable
            val deviceName = backStackEntry.arguments?.getString("deviceName") ?: "Device"
            LiveLocationScreen(
                deviceId = deviceId,
                deviceName = deviceName,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        // Live Stream Screen
        composable(
            route = ParentScreen.LiveStream.route,
            arguments = listOf(
                navArgument("deviceId") { type = NavType.StringType },
                navArgument("deviceName") { type = NavType.StringType },
                navArgument("streamType") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val deviceId = backStackEntry.arguments?.getString("deviceId") ?: return@composable
            val deviceName = backStackEntry.arguments?.getString("deviceName") ?: "Device"
            val streamType = backStackEntry.arguments?.getString("streamType") ?: "camera"
            StreamViewerScreen(
                childDeviceId = deviceId,
                childName = deviceName,
                initialStreamType = streamType,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        // Snapshots Screen
        composable(
            route = ParentScreen.Snapshots.route,
            arguments = listOf(
                navArgument("deviceId") { type = NavType.StringType },
                navArgument("deviceName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val deviceId = backStackEntry.arguments?.getString("deviceId") ?: return@composable
            val deviceName = backStackEntry.arguments?.getString("deviceName") ?: "Device"
            SnapshotsScreen(
                deviceId = deviceId,
                deviceName = deviceName,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
