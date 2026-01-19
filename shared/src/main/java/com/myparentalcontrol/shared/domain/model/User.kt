package com.myparentalcontrol.shared.domain.model

data class User(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = System.currentTimeMillis(),
    val fcmToken: String = "",
    val devices: List<String> = emptyList() // List of device IDs
)

data class PairingCode(
    val code: String = "",
    val parentId: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + 300000, // 5 minutes
    val isUsed: Boolean = false,
    val usedByDeviceId: String = ""
)

data class AuthState(
    val isLoggedIn: Boolean = false,
    val user: User? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)
