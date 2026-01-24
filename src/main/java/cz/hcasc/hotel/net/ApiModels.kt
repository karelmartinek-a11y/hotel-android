package cz.hcasc.hotel.net

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * API models for https://hotel.hcasc.cz
 *
 * Notes:
 * - Android never uses passwords.
 * - Device authentication uses a device token (issued after activation) + challenge-response signature.
 * - JSON field names are deterministic and stable; keep backward compatible.
 */

@Serializable
enum class DeviceStatus { @SerialName("PENDING") PENDING, @SerialName("ACTIVE") ACTIVE, @SerialName("REVOKED") REVOKED }

@Serializable
enum class ReportType { @SerialName("FIND") FIND, @SerialName("ISSUE") ISSUE }

@Serializable
enum class ReportState { @SerialName("OPEN") OPEN, @SerialName("DONE") DONE }

@Serializable
data class ApiError(
    val code: String,
    val message: String,
    val details: String? = null
)

// ----------------------
// Device registration / status / challenge / verify
// ----------------------

@Serializable
data class DeviceRegisterRequest(
    /** Locally generated UUID on first install. */
    val deviceId: String,
    /** Public key encoded as base64 (raw) or PEM-like string; backend defines accepted format. */
    val publicKey: String,
    /** Optional friendly name for admin schválení. */
    @SerialName("display_name")
    val displayName: String? = null,
    /** Optional informational map: model/manufacturer/appVersion. */
    val deviceInfo: Map<String, String?>? = null
)

@Serializable
data class DeviceRegisterResponse(
    val deviceId: String,
    val status: DeviceStatus
)

@Serializable
data class DeviceStatusResponse(
    val status: DeviceStatus,
    /** Present only when ACTIVE (backend may also include when PENDING for admin UI). */
    val activatedAt: String? = null,
    @SerialName("display_name")
    val displayName: String? = null,
    @SerialName("device_id")
    val deviceId: String? = null
)

@Serializable
data class DeviceChallengeRequest(
    val deviceId: String
)

@Serializable
data class DeviceChallengeResponse(
    /** Unique nonce (base64) to be signed by the device. */
    val nonce: String,
    /** ISO timestamp for UI/logging; do not rely on device clock. */
    val issuedAt: String
)

@Serializable
data class DeviceVerifyRequest(
    /** Signed nonce (base64). */
    val signature: String,
    /** Echoed nonce to bind request. */
    val nonce: String
)

@Serializable
data class DeviceVerifyResponse(
    /** Device token issued after successful verification while ACTIVE. */
    val deviceToken: String,
    val status: DeviceStatus
)

// ----------------------
// Reports
// ----------------------

@Serializable
data class CreateReportMetadata(
    val type: ReportType,
    /** Room number e.g. 101..109, 201..210, 301..310 */
    val room: Int,
    /** Optional short description; max 50 chars on server too. */
    val description: String? = null,
    /** Client timestamp (ISO). Server stores its own created_at as source of truth. */
    val clientCreatedAt: String? = null
)

@Serializable
data class CreateReportResponse(
    val reportId: String,
    val ok: Boolean
)

@Serializable
data class ReportListResponse(val items: List<ReportSummary> = emptyList())

@Serializable
data class MarkDoneRequest(
    val reportId: String
)

@Serializable
data class MarkDoneResponse(
    val ok: Boolean
)

@Serializable
data class ReopenRequest(
    val reportId: String
)

@Serializable
data class ReopenResponse(
    val ok: Boolean
)

@Serializable
data class DeleteReportRequest(
    val reportId: String
)

@Serializable
data class DeleteReportResponse(
    val ok: Boolean
)

// ----------------------
// Polling - new since last seen
// ----------------------

@Serializable
data class NewSinceResponse(
    val lastSeenOpenFindsId: Long? = null,
    val lastSeenOpenIssuesId: Long? = null,
    val newOpenFindsCount: Int = 0,
    val newOpenIssuesCount: Int = 0
)

// Generic helpers
@Serializable
data class ReportSummary(
    val id: String,
    val room: Int,
    val description: String? = null,
    val createdAt: String = "",
    val type: ReportType = ReportType.FIND,
    val photos: List<String> = emptyList(),
    val thumbnailUrls: List<String> = emptyList()
)

@Serializable
data class GenericOkResponse(val ok: Boolean = true)
