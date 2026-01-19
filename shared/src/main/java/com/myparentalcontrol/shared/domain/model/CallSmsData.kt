package com.myparentalcontrol.shared.domain.model

data class CallLogEntry(
    val id: String = "",
    val deviceId: String = "",
    val phoneNumber: String = "",
    val contactName: String = "", // If available from contacts
    val callType: CallType = CallType.UNKNOWN,
    val duration: Long = 0, // in seconds
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false // For parent app to track if viewed
)

enum class CallType {
    INCOMING,
    OUTGOING,
    MISSED,
    REJECTED,
    BLOCKED,
    UNKNOWN
}

data class SmsLogEntry(
    val id: String = "",
    val deviceId: String = "",
    val phoneNumber: String = "",
    val contactName: String = "", // If available from contacts
    val messageBody: String = "",
    val smsType: SmsType = SmsType.UNKNOWN,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false // For parent app to track if viewed
)

enum class SmsType {
    INCOMING,
    OUTGOING,
    DRAFT,
    UNKNOWN
}

data class ContactInfo(
    val phoneNumber: String = "",
    val contactName: String = "",
    val photoUri: String = ""
)
