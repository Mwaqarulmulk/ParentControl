package com.myparentalcontrol.shared.domain.model

data class LocationData(
    val id: String = "",
    val deviceId: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val accuracy: Float = 0f,
    val altitude: Double = 0.0,
    val speed: Float = 0f,
    val bearing: Float = 0f,
    val address: String = "", // Reverse geocoded address (optional)
    val timestamp: Long = System.currentTimeMillis(),
    val provider: String = "" // "gps", "network", "fused"
)

data class LocationHistory(
    val deviceId: String = "",
    val locations: List<LocationData> = emptyList(),
    val date: String = "" // Format: "yyyy-MM-dd"
)

data class Geofence(
    val id: String = "",
    val name: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val radius: Float = 100f, // meters
    val isEnabled: Boolean = true,
    val notifyOnEnter: Boolean = true,
    val notifyOnExit: Boolean = true
)
