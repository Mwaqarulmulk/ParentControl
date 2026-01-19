package com.myparentalcontrol.shared.data.supabase

import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.realtime.PostgresAction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseRepository @Inject constructor(
    private val supabaseClient: SupabaseClient
) {
    companion object {
        private const val TAG = "SupabaseRepository"
    }

    // ==========================================
    // DEVICES
    // ==========================================
    
    suspend fun registerDevice(
        deviceId: String,
        deviceName: String,
        deviceModel: String?,
        androidVersion: String?,
        appVersion: String?
    ): Result<Unit> = runCatching {
        supabaseClient.postgrest[SupabaseConfig.Tables.DEVICES].upsert(
            mapOf(
                "device_id" to deviceId,
                "device_name" to deviceName,
                "device_model" to deviceModel,
                "android_version" to androidVersion,
                "app_version" to appVersion,
                "is_online" to true,
                "last_seen" to System.currentTimeMillis()
            )
        )
        Log.d(TAG, "Device registered: $deviceId")
    }

    suspend fun updateDeviceStatus(
        deviceId: String,
        isOnline: Boolean,
        batteryLevel: Int? = null,
        isCharging: Boolean? = null,
        networkType: String? = null
    ): Result<Unit> = runCatching {
        val updates = mutableMapOf<String, Any?>(
            "is_online" to isOnline,
            "last_seen" to System.currentTimeMillis()
        )
        batteryLevel?.let { updates["battery_level"] = it }
        isCharging?.let { updates["is_charging"] = it }
        networkType?.let { updates["network_type"] = it }

        supabaseClient.postgrest[SupabaseConfig.Tables.DEVICES]
            .update(updates) {
                filter { eq("device_id", deviceId) }
            }
        Log.d(TAG, "Device status updated: $deviceId, online=$isOnline")
    }

    suspend fun getDevice(deviceId: String): Result<DeviceData?> = runCatching {
        supabaseClient.postgrest[SupabaseConfig.Tables.DEVICES]
            .select {
                filter { eq("device_id", deviceId) }
            }
            .decodeSingleOrNull<DeviceData>()
    }

    suspend fun getDevicesForParent(parentId: String): Result<List<DeviceData>> = runCatching {
        supabaseClient.postgrest[SupabaseConfig.Tables.DEVICES]
            .select {
                filter { eq("parent_id", parentId) }
            }
            .decodeList<DeviceData>()
    }

    suspend fun pairDeviceWithParent(deviceId: String, parentId: String): Result<Unit> = runCatching {
        supabaseClient.postgrest[SupabaseConfig.Tables.DEVICES]
            .update(mapOf(
                "parent_id" to parentId,
                "paired_at" to System.currentTimeMillis()
            )) {
                filter { eq("device_id", deviceId) }
            }
        Log.d(TAG, "Device paired: $deviceId with parent $parentId")
    }

    // ==========================================
    // PAIRING CODES
    // ==========================================

    suspend fun createPairingCode(
        code: String,
        deviceId: String,
        deviceName: String?,
        expiresInMinutes: Int = 10
    ): Result<Unit> = runCatching {
        val expiresAt = System.currentTimeMillis() + (expiresInMinutes * 60 * 1000)
        supabaseClient.postgrest[SupabaseConfig.Tables.PAIRING_CODES].insert(
            mapOf(
                "code" to code,
                "device_id" to deviceId,
                "device_name" to deviceName,
                "expires_at" to expiresAt
            )
        )
        Log.d(TAG, "Pairing code created: $code for device $deviceId")
    }

    suspend fun getPairingCode(code: String): Result<PairingCodeData?> = runCatching {
        supabaseClient.postgrest[SupabaseConfig.Tables.PAIRING_CODES]
            .select {
                filter { 
                    eq("code", code)
                    eq("is_used", false)
                }
            }
            .decodeSingleOrNull<PairingCodeData>()
    }

    suspend fun markPairingCodeUsed(code: String): Result<Unit> = runCatching {
        supabaseClient.postgrest[SupabaseConfig.Tables.PAIRING_CODES]
            .update(mapOf("is_used" to true)) {
                filter { eq("code", code) }
            }
    }

    // ==========================================
    // LOCATIONS
    // ==========================================

    suspend fun insertLocation(
        deviceId: String,
        latitude: Double,
        longitude: Double,
        accuracy: Float? = null,
        altitude: Double? = null,
        speed: Float? = null,
        provider: String? = null,
        address: String? = null
    ): Result<Unit> = runCatching {
        supabaseClient.postgrest[SupabaseConfig.Tables.LOCATIONS].insert(
            mapOf(
                "device_id" to deviceId,
                "latitude" to latitude,
                "longitude" to longitude,
                "accuracy" to accuracy,
                "altitude" to altitude,
                "speed" to speed,
                "provider" to provider,
                "address" to address,
                "recorded_at" to System.currentTimeMillis()
            )
        )
        Log.d(TAG, "Location inserted for device $deviceId: $latitude, $longitude")
    }

    suspend fun getLocations(
        deviceId: String,
        limit: Int = 100
    ): Result<List<LocationData>> = runCatching {
        supabaseClient.postgrest[SupabaseConfig.Tables.LOCATIONS]
            .select {
                filter { eq("device_id", deviceId) }
                order("recorded_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                limit(limit.toLong())
            }
            .decodeList<LocationData>()
    }

    suspend fun getLatestLocation(deviceId: String): Result<LocationData?> = runCatching {
        supabaseClient.postgrest[SupabaseConfig.Tables.LOCATIONS]
            .select {
                filter { eq("device_id", deviceId) }
                order("recorded_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                limit(1)
            }
            .decodeSingleOrNull<LocationData>()
    }

    // ==========================================
    // COMMANDS
    // ==========================================

    suspend fun createCommand(
        deviceId: String,
        commandType: String,
        payload: Map<String, Any> = emptyMap()
    ): Result<String> = runCatching {
        val result = supabaseClient.postgrest[SupabaseConfig.Tables.COMMANDS].insert(
            mapOf(
                "device_id" to deviceId,
                "command_type" to commandType,
                "payload" to payload,
                "status" to "pending"
            )
        ) {
            select()
        }.decodeSingle<CommandData>()
        Log.d(TAG, "Command created: ${result.id} - $commandType for $deviceId")
        result.id
    }

    suspend fun getPendingCommands(deviceId: String): Result<List<CommandData>> = runCatching {
        supabaseClient.postgrest[SupabaseConfig.Tables.COMMANDS]
            .select {
                filter { 
                    eq("device_id", deviceId)
                    eq("status", "pending")
                }
                order("created_at", io.github.jan.supabase.postgrest.query.Order.ASCENDING)
            }
            .decodeList<CommandData>()
    }

    suspend fun updateCommandStatus(
        commandId: String,
        status: String,
        result: Map<String, Any>? = null
    ): Result<Unit> = runCatching {
        val updates = mutableMapOf<String, Any?>(
            "status" to status
        )
        if (status == "executing") {
            updates["executed_at"] = System.currentTimeMillis()
        }
        if (status == "completed" || status == "failed") {
            updates["completed_at"] = System.currentTimeMillis()
            result?.let { updates["result"] = it }
        }
        
        supabaseClient.postgrest[SupabaseConfig.Tables.COMMANDS]
            .update(updates) {
                filter { eq("id", commandId) }
            }
        Log.d(TAG, "Command $commandId status updated to $status")
    }

    // ==========================================
    // SNAPSHOTS
    // ==========================================

    suspend fun insertSnapshot(
        deviceId: String,
        snapshotType: String,
        imageData: String? = null,
        storagePath: String? = null,
        width: Int? = null,
        height: Int? = null,
        fileSize: Int? = null
    ): Result<Unit> = runCatching {
        supabaseClient.postgrest[SupabaseConfig.Tables.SNAPSHOTS].insert(
            mapOf(
                "device_id" to deviceId,
                "snapshot_type" to snapshotType,
                "image_data" to imageData,
                "storage_path" to storagePath,
                "width" to width,
                "height" to height,
                "file_size" to fileSize,
                "captured_at" to System.currentTimeMillis()
            )
        )
        Log.d(TAG, "Snapshot inserted for device $deviceId: $snapshotType")
    }

    suspend fun getSnapshots(
        deviceId: String,
        limit: Int = 50
    ): Result<List<SnapshotData>> = runCatching {
        supabaseClient.postgrest[SupabaseConfig.Tables.SNAPSHOTS]
            .select {
                filter { eq("device_id", deviceId) }
                order("captured_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                limit(limit.toLong())
            }
            .decodeList<SnapshotData>()
    }

    // ==========================================
    // ALERTS
    // ==========================================

    suspend fun createAlert(
        deviceId: String,
        alertType: String,
        title: String,
        message: String? = null,
        severity: String = "info",
        metadata: Map<String, Any> = emptyMap()
    ): Result<Unit> = runCatching {
        supabaseClient.postgrest[SupabaseConfig.Tables.ALERTS].insert(
            mapOf(
                "device_id" to deviceId,
                "alert_type" to alertType,
                "title" to title,
                "message" to message,
                "severity" to severity,
                "metadata" to metadata
            )
        )
        Log.d(TAG, "Alert created for device $deviceId: $title")
    }

    suspend fun getUnreadAlerts(deviceId: String): Result<List<AlertData>> = runCatching {
        supabaseClient.postgrest[SupabaseConfig.Tables.ALERTS]
            .select {
                filter { 
                    eq("device_id", deviceId)
                    eq("is_read", false)
                }
                order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
            }
            .decodeList<AlertData>()
    }

    suspend fun markAlertRead(alertId: String): Result<Unit> = runCatching {
        supabaseClient.postgrest[SupabaseConfig.Tables.ALERTS]
            .update(mapOf("is_read" to true)) {
                filter { eq("id", alertId) }
            }
    }

    // ==========================================
    // REALTIME SUBSCRIPTIONS
    // ==========================================

    suspend fun subscribeToCommands(deviceId: String): Flow<CommandData> {
        val channel = supabaseClient.realtime.channel("commands-$deviceId")
        
        val flow = channel.postgresChangeFlow<PostgresAction.Insert>(
            schema = "public"
        ) {
            table = SupabaseConfig.Tables.COMMANDS
            @Suppress("DEPRECATION")
            filter = "device_id=eq.$deviceId"
        }.map { change ->
            change.record.let { record ->
                CommandData(
                    id = record["id"].toString(),
                    deviceId = record["device_id"].toString(),
                    commandType = record["command_type"].toString(),
                    status = record["status"].toString(),
                    createdAt = record["created_at"]?.toString()
                )
            }
        }
        
        channel.subscribe()
        return flow
    }

    suspend fun subscribeToDeviceStatus(deviceId: String): Flow<DeviceData> {
        val channel = supabaseClient.realtime.channel("device-$deviceId")
        
        val flow = channel.postgresChangeFlow<PostgresAction.Update>(
            schema = "public"
        ) {
            table = SupabaseConfig.Tables.DEVICES
            @Suppress("DEPRECATION")
            filter = "device_id=eq.$deviceId"
        }.map { change ->
            change.record.let { record ->
                DeviceData(
                    id = record["id"].toString(),
                    deviceId = record["device_id"].toString(),
                    deviceName = record["device_name"]?.toString(),
                    isOnline = record["is_online"]?.toString()?.toBoolean() ?: false,
                    batteryLevel = record["battery_level"]?.toString()?.toIntOrNull(),
                    isCharging = record["is_charging"]?.toString()?.toBoolean(),
                    networkType = record["network_type"]?.toString()
                )
            }
        }
        
        channel.subscribe()
        return flow
    }
}

// ==========================================
// DATA CLASSES
// ==========================================

@Serializable
data class DeviceData(
    val id: String? = null,
    val deviceId: String? = null,
    val parentId: String? = null,
    val deviceName: String? = null,
    val deviceModel: String? = null,
    val androidVersion: String? = null,
    val appVersion: String? = null,
    val isOnline: Boolean = false,
    val lastSeen: Long? = null,
    val batteryLevel: Int? = null,
    val isCharging: Boolean? = null,
    val networkType: String? = null,
    val pairedAt: Long? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Serializable
data class PairingCodeData(
    val id: String? = null,
    val code: String,
    val deviceId: String,
    val deviceName: String? = null,
    val isUsed: Boolean = false,
    val expiresAt: Long,
    val createdAt: String? = null
)

@Serializable
data class LocationData(
    val id: String? = null,
    val deviceId: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float? = null,
    val altitude: Double? = null,
    val speed: Float? = null,
    val bearing: Float? = null,
    val provider: String? = null,
    val address: String? = null,
    val recordedAt: Long? = null,
    val createdAt: String? = null
)

@Serializable
data class CommandData(
    val id: String,
    val deviceId: String? = null,
    val commandType: String,
    val payload: JsonObject? = null,
    val status: String = "pending",
    val result: JsonObject? = null,
    val createdAt: String? = null,
    val executedAt: String? = null,
    val completedAt: String? = null
)

@Serializable
data class SnapshotData(
    val id: String? = null,
    val deviceId: String,
    val snapshotType: String,
    val imageData: String? = null,
    val storagePath: String? = null,
    val thumbnailPath: String? = null,
    val fileSize: Int? = null,
    val width: Int? = null,
    val height: Int? = null,
    val capturedAt: Long? = null,
    val createdAt: String? = null
)

@Serializable
data class AlertData(
    val id: String? = null,
    val deviceId: String,
    val alertType: String,
    val title: String,
    val message: String? = null,
    val severity: String = "info",
    val isRead: Boolean = false,
    val metadata: JsonObject? = null,
    val createdAt: String? = null
)
