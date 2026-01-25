package cz.hcasc.hotel.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Offline queued report.
 * Spec: app neukládá historii, jen dočasnou frontu (1..5 fotek).
 */
@Entity(
    tableName = "queued_reports",
    indices = [Index("created_at_ms")]
)
data class QueuedReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "local_uuid") val localUuid: String,
    @ColumnInfo(name = "type") val type: String,
    @ColumnInfo(name = "room") val room: Int,
    @ColumnInfo(name = "description") val description: String?,
    @ColumnInfo(name = "created_at_ms") val createdAtEpochMs: Long,
    @ColumnInfo(name = "last_error") val lastError: String? = null
)

/**
 * Offline queued photo belonging to a report.
 */
@Entity(
    tableName = "queued_photos",
    indices = [Index("queued_report_id")]
)
data class QueuedPhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "queued_report_id") val queuedReportId: Long,
    @ColumnInfo(name = "photo_index") val index: Int,
    @ColumnInfo(name = "local_path") val localPath: String,
    @ColumnInfo(name = "mime_type") val mimeType: String = "image/jpeg",
    @ColumnInfo(name = "size_bytes") val sizeBytes: Long = 0
)

/**
 * Single-row table se stavem zařízení a last-seen markery pro polling.
 */
@Entity(tableName = "device_state")
data class DeviceStateEntity(
    @PrimaryKey val id: Int = 1,
    @ColumnInfo(name = "device_id") val deviceId: String? = null,
    @ColumnInfo(name = "status") val status: String = "PENDING",
    @ColumnInfo(name = "device_token") val deviceToken: String? = null,
    @ColumnInfo(name = "last_status_check_at") val lastStatusCheckAt: String? = null,
    @ColumnInfo(name = "last_seen_open_finds_id") val lastSeenOpenFindsId: Long? = null,
    @ColumnInfo(name = "last_seen_open_issues_id") val lastSeenOpenIssuesId: Long? = null
)
