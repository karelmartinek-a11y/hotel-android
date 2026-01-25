package cz.hcasc.hotel.repo

import android.content.Context
import android.util.Base64
import cz.hcasc.hotel.App
import cz.hcasc.hotel.db.AppDatabase
import cz.hcasc.hotel.db.DeviceStateEntity
import cz.hcasc.hotel.net.DeviceRegisterRequest
import cz.hcasc.hotel.net.HotelApi
import cz.hcasc.hotel.security.ChallengeSigner
import cz.hcasc.hotel.security.DeviceIdentity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import java.time.Instant

data class DeviceUiState(
    val isActivated: Boolean,
    val statusText: String,
    val deviceId: String
)

data class ActivationSnapshot(val isActivated: Boolean, val deviceId: String?)
data class LastSeenSnapshot(val lastSeenOpenFindsId: Long?, val lastSeenOpenIssuesId: Long?)

class DeviceRepo(
    private val appContext: Context,
    private val db: AppDatabase,
    private val api: HotelApi,
    private val identity: DeviceIdentity,
    private val signer: ChallengeSigner
) {
    companion object {
        fun from(context: Context): DeviceRepo =
            (context.applicationContext as App).deviceRepo
    }

    private val _deviceState = MutableStateFlow(
        DeviceUiState(
            isActivated = false,
            statusText = "Aktivujte zařízení",
            deviceId = ""
        )
    )
    val deviceState: StateFlow<DeviceUiState> = _deviceState

    suspend fun refreshStateBestEffort() {
        val idValue = identity.getOrCreateDeviceId(appContext)
        val state = db.deviceStateDao().get()
        if (state.deviceId == null) {
            db.deviceStateDao().upsert(state.copy(deviceId = idValue, status = "PENDING"))
        }
        // Pokus o registraci (uloží display_name pro admina); neblokuje případné chyby.
        runCatching {
            registerDevice(idValue)
        }
        // Try status call; ignore failures
        runCatching {
            val resp = api.deviceStatus(deviceId = idValue)
            resp.displayName?.let { identity.setDisplayName(appContext, it) }
            db.deviceStateDao().upsert(
                state.copy(
                    deviceId = resp.deviceId ?: idValue,
                    status = resp.status.name,
                    lastStatusCheckAt = Instant.now().toString()
                )
            )
        }
        publishState()
    }

    private suspend fun publishState() {
        val st = db.deviceStateDao().get()
        val activated = st.status == "ACTIVE"
        _deviceState.update {
            DeviceUiState(
                isActivated = activated,
                statusText = when (st.status) {
                    "ACTIVE" -> "Vítejte doma"
                    "REVOKED" -> "Zablokováno správcem"
                    else -> "Aktivujte zařízení"
                },
                deviceId = st.deviceId ?: ""
            )
        }
    }

    suspend fun getActivationSnapshot(): ActivationSnapshot {
        val st = db.deviceStateDao().get()
        return ActivationSnapshot(st.status == "ACTIVE", st.deviceId)
    }

    suspend fun getLastSeenSnapshot(): LastSeenSnapshot {
        val st = db.deviceStateDao().get()
        return LastSeenSnapshot(st.lastSeenOpenFindsId, st.lastSeenOpenIssuesId)
    }

    suspend fun updateLastSeen(lastSeenOpenFindsId: Long?, lastSeenOpenIssuesId: Long?) {
        val st = db.deviceStateDao().get()
        db.deviceStateDao().upsert(
            st.copy(
                lastSeenOpenFindsId = lastSeenOpenFindsId,
                lastSeenOpenIssuesId = lastSeenOpenIssuesId
            )
        )
    }

    suspend fun markDeactivatedFromServer() {
        val st = db.deviceStateDao().get()
        db.deviceStateDao().upsert(st.copy(status = "REVOKED", deviceToken = null))
        publishState()
    }

    fun getDeviceTokenOrNull(): String? = runBlocking { db.deviceStateDao().get().deviceToken }
    fun getDeviceIdOrNull(): String? = runBlocking {
        val state = db.deviceStateDao().get()
        state.deviceId ?: identity.getOrCreateDeviceId(appContext)
    }
    fun getDisplayName(): String = identity.getDisplayName(appContext) ?: ""

    suspend fun saveDisplayName(name: String) {
        identity.setDisplayName(appContext, name)
        refreshStateBestEffort()
    }

    fun api(): HotelApi = api

    // Placeholder for auth header; attach bearer token if present.
    fun getAuthHeaderOrNull(): String? = getDeviceTokenOrNull()?.let { "Bearer $it" }

    private suspend fun registerDevice(deviceId: String) {
        val publicKeyBytes = identity.getPublicKey().encoded
        val publicKeyB64 = Base64.encodeToString(publicKeyBytes, Base64.NO_WRAP)
        val displayName = identity.getDisplayName(appContext)?.takeIf { it.isNotBlank() }
        val deviceInfo = identity.deviceInfo().mapValues { it.value?.toString() }
        val resp = api.deviceRegister(
            DeviceRegisterRequest(
                deviceId = deviceId,
                publicKey = publicKeyB64,
                displayName = displayName,
                deviceInfo = deviceInfo
            )
        )
        resp.displayName?.let { identity.setDisplayName(appContext, it) }

        val state = db.deviceStateDao().get()
        db.deviceStateDao().upsert(
            state.copy(
                deviceId = resp.deviceId ?: deviceId,
                status = resp.status.name,
                lastStatusCheckAt = Instant.now().toString()
            )
        )
    }
}
