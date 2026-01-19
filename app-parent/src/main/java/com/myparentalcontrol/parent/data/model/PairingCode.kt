package com.myparentalcontrol.parent.data.model

/**
 * Pairing Code Model
 * Used to pair parent and child devices
 */
data class PairingCode(
    val code: String = "",
    val parentId: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + (5 * 60 * 1000), // 5 minutes
    val isUsed: Boolean = false,
    val childDeviceId: String? = null
) {
    fun isExpired(): Boolean = System.currentTimeMillis() > expiresAt
    
    fun isValid(): Boolean = !isUsed && !isExpired()
    
    companion object {
        fun generate(parentId: String): PairingCode {
            val code = generateRandomCode()
            return PairingCode(
                code = code,
                parentId = parentId
            )
        }
        
        private fun generateRandomCode(): String {
            // Generate 6-digit code
            return (100000..999999).random().toString()
        }
    }
}
