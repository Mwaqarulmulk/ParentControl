package com.myparentalcontrol.child.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.myparentalcontrol.child.presentation.screens.setup.SetupScreen
import com.myparentalcontrol.child.presentation.screens.status.StatusScreen
import com.myparentalcontrol.child.presentation.screens.permissions.PermissionsScreen

sealed class ChildScreen(val route: String) {
    object Setup : ChildScreen("setup")
    object Permissions : ChildScreen("permissions")
    object Status : ChildScreen("status")
}

@Composable
fun ChildNavigation() {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = ChildScreen.Setup.route
    ) {
        composable(ChildScreen.Setup.route) {
            SetupScreen(
                onSetupComplete = {
                    navController.navigate(ChildScreen.Permissions.route) {
                        popUpTo(ChildScreen.Setup.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(ChildScreen.Permissions.route) {
            PermissionsScreen(
                onPermissionsGranted = {
                    navController.navigate(ChildScreen.Status.route) {
                        popUpTo(ChildScreen.Permissions.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(ChildScreen.Status.route) {
            StatusScreen()
        }
    }
}
