package com.myparentalcontrol.parent.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.myparentalcontrol.parent.presentation.screens.child.ChildDetailsScreen
import com.myparentalcontrol.parent.presentation.screens.dashboard.DashboardScreen
import com.myparentalcontrol.parent.presentation.screens.location.LocationScreen
import com.myparentalcontrol.parent.presentation.screens.notifications.NotificationsScreen
import com.myparentalcontrol.parent.presentation.screens.snapshots.SnapshotsScreen
import com.myparentalcontrol.parent.streaming.ui.StreamViewerScreen

/**
 * Navigation routes for parent app
 */
sealed class ParentRoute(val route: String) {
    object Dashboard : ParentRoute("dashboard")
    object StreamViewer : ParentRoute("stream_viewer/{childDeviceId}/{childName}") {
        fun createRoute(childDeviceId: String, childName: String): String {
            return "stream_viewer/$childDeviceId/$childName"
        }
    }
    object ChildDetails : ParentRoute("child_details/{childDeviceId}/{childName}") {
        fun createRoute(childDeviceId: String, childName: String): String {
            return "child_details/$childDeviceId/$childName"
        }
    }
    object Notifications : ParentRoute("notifications/{childDeviceId}/{childName}") {
        fun createRoute(childDeviceId: String, childName: String): String {
            return "notifications/$childDeviceId/$childName"
        }
    }
    object Location : ParentRoute("location/{childDeviceId}/{childName}") {
        fun createRoute(childDeviceId: String, childName: String): String {
            return "location/$childDeviceId/$childName"
        }
    }
    object Snapshots : ParentRoute("snapshots/{childDeviceId}/{childName}") {
        fun createRoute(childDeviceId: String, childName: String): String {
            return "snapshots/$childDeviceId/$childName"
        }
    }
    object Settings : ParentRoute("settings")
    object AddChild : ParentRoute("add_child")
}

/**
 * Main navigation host for parent app
 */
@Composable
fun ParentNavigation() {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = ParentRoute.Dashboard.route
    ) {
        // Dashboard - main screen showing all children
        composable(ParentRoute.Dashboard.route) {
            DashboardScreen(
                onNavigateToChild = { childDeviceId, childName ->
                    navController.navigate(ParentRoute.ChildDetails.createRoute(childDeviceId, childName))
                },
                onNavigateToStream = { childDeviceId, childName ->
                    navController.navigate(ParentRoute.StreamViewer.createRoute(childDeviceId, childName))
                },
                onNavigateToAddChild = {
                    navController.navigate(ParentRoute.AddChild.route)
                },
                onNavigateToSettings = {
                    navController.navigate(ParentRoute.Settings.route)
                }
            )
        }
        
        // Stream Viewer
        composable(
            route = ParentRoute.StreamViewer.route,
            arguments = listOf(
                navArgument("childDeviceId") { type = NavType.StringType },
                navArgument("childName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val childDeviceId = backStackEntry.arguments?.getString("childDeviceId") ?: ""
            val childName = backStackEntry.arguments?.getString("childName") ?: "Child"
            
            StreamViewerScreen(
                childDeviceId = childDeviceId,
                childName = childName,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // Child Details - Full monitoring dashboard
        composable(
            route = ParentRoute.ChildDetails.route,
            arguments = listOf(
                navArgument("childDeviceId") { type = NavType.StringType },
                navArgument("childName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val childDeviceId = backStackEntry.arguments?.getString("childDeviceId") ?: ""
            val childName = backStackEntry.arguments?.getString("childName") ?: "Child"
            
            ChildDetailsScreen(
                childDeviceId = childDeviceId,
                childName = childName,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToStream = { deviceId, name ->
                    navController.navigate(ParentRoute.StreamViewer.createRoute(deviceId, name))
                },
                onNavigateToNotifications = { deviceId, name ->
                    navController.navigate(ParentRoute.Notifications.createRoute(deviceId, name))
                },
                onNavigateToLocation = { deviceId, name ->
                    navController.navigate(ParentRoute.Location.createRoute(deviceId, name))
                },
                onNavigateToSnapshots = { deviceId, name ->
                    navController.navigate(ParentRoute.Snapshots.createRoute(deviceId, name))
                }
            )
        }
        
        // Notifications Screen
        composable(
            route = ParentRoute.Notifications.route,
            arguments = listOf(
                navArgument("childDeviceId") { type = NavType.StringType },
                navArgument("childName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val childDeviceId = backStackEntry.arguments?.getString("childDeviceId") ?: ""
            val childName = backStackEntry.arguments?.getString("childName") ?: "Child"
            
            NotificationsScreen(
                childDeviceId = childDeviceId,
                childName = childName,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // Location Screen with Map
        composable(
            route = ParentRoute.Location.route,
            arguments = listOf(
                navArgument("childDeviceId") { type = NavType.StringType },
                navArgument("childName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val childDeviceId = backStackEntry.arguments?.getString("childDeviceId") ?: ""
            val childName = backStackEntry.arguments?.getString("childName") ?: "Child"
            
            LocationScreen(
                childDeviceId = childDeviceId,
                childName = childName,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // Snapshots Gallery Screen
        composable(
            route = ParentRoute.Snapshots.route,
            arguments = listOf(
                navArgument("childDeviceId") { type = NavType.StringType },
                navArgument("childName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val childDeviceId = backStackEntry.arguments?.getString("childDeviceId") ?: ""
            val childName = backStackEntry.arguments?.getString("childName") ?: "Child"
            
            SnapshotsScreen(
                childDeviceId = childDeviceId,
                childName = childName,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // Settings (placeholder)
        composable(ParentRoute.Settings.route) {
            // TODO: Implement SettingsScreen
            DashboardScreen(
                onNavigateToChild = { _, _ -> },
                onNavigateToStream = { _, _ -> },
                onNavigateToAddChild = {},
                onNavigateToSettings = {}
            )
        }
        
        // Add Child (placeholder)
        composable(ParentRoute.AddChild.route) {
            // TODO: Implement AddChildScreen
            DashboardScreen(
                onNavigateToChild = { _, _ -> },
                onNavigateToStream = { _, _ -> },
                onNavigateToAddChild = {},
                onNavigateToSettings = {}
            )
        }
    }
}
