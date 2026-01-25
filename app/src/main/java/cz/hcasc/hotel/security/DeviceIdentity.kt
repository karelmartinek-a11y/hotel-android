package cz.hcasc.hotel.security

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import java.util.UUID

/**
 * Device identity for HOTEL Android app.
 *
 * Requirements:
 * - No passwords in Android.
 * - Stable device_id (UUID generated at install) used to identify device on server.
 * - Asymmetric keypair stored in Android Keystore to prevent cloning.
 * - Challenge-response signing: server sends nonce, app signs it, server verifies.
 *
 * Notes:
 * - We use EC key (P-256) because it is broadly supported on Android Keystore.
 *   Ed25519 is not guaranteed on all API levels/devices.
 * - We deliberately store ONLY device_id and device_token (in separate store) persistently.
 */
object DeviceIdentity {
    private const val PREFS_FILE = "hotel_device_identity"
    private const val KEY_DEVICE_ID = "device_id"
    private const val KEY_DISPLAY_NAME = "display_name"

    // Keystore alias for the device signing key.
    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val KEY_ALIAS = "cz.hcasc.hotel.device_signing"

    /**
     * Returns stable device_id (UUID string). Generated once and stored securely.
     */
    fun getOrCreateDeviceId(context: Context): String {
        val prefs = securePrefs(context)
        val existing = prefs.getString(KEY_DEVICE_ID, null)
        if (!existing.isNullOrBlank()) return existing

        val newId = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_DEVICE_ID, newId).apply()
        return newId
    }

    /**
     * Ensures keystore keypair exists and returns it.
     */
    fun getOrCreateKeyPair(): KeyPair {
        val ks = KeyStore.getInstance(KEYSTORE_PROVIDER)
        ks.load(null)

        if (ks.containsAlias(KEY_ALIAS)) {
            val privateKey = ks.getKey(KEY_ALIAS, null) as PrivateKey
            val publicKey = ks.getCertificate(KEY_ALIAS).publicKey
            return KeyPair(publicKey, privateKey)
        }

        // Create new keypair.
        // Use Android Keystore with EC key for signatures.
        val kpg = KeyPairGenerator.getInstance(
            android.security.keystore.KeyProperties.KEY_ALGORITHM_EC,
            KEYSTORE_PROVIDER
        )

        val spec = android.security.keystore.KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            android.security.keystore.KeyProperties.PURPOSE_SIGN or android.security.keystore.KeyProperties.PURPOSE_VERIFY
        )
            .setDigests(
                android.security.keystore.KeyProperties.DIGEST_SHA256,
                android.security.keystore.KeyProperties.DIGEST_SHA512
            )
            .setUserAuthenticationRequired(false)
            // Prefer StrongBox if available; do not hard-require it.
            .setIsStrongBoxBacked(isStrongBoxSupported())
            .build()

        kpg.initialize(spec)
        return kpg.generateKeyPair()
    }

    fun getPublicKey(): PublicKey = getOrCreateKeyPair().public

    /**
     * Signs nonce bytes using ECDSA with SHA-256.
     *
     * Server should verify against the registered public key.
     */
    fun signNonce(nonce: ByteArray): ByteArray {
        val kp = getOrCreateKeyPair()
        val sig = Signature.getInstance("SHA256withECDSA")
        sig.initSign(kp.private)
        sig.update(nonce)
        return sig.sign()
    }

    /**
     * Convenience: create a stable device_info snapshot for register call.
     * Keep it minimal; do not include sensitive data.
     */
    fun deviceInfo(): Map<String, Any?> {
        return mapOf(
            "manufacturer" to Build.MANUFACTURER,
            "model" to Build.MODEL,
            "sdk_int" to Build.VERSION.SDK_INT,
            "release" to Build.VERSION.RELEASE
        )
    }

    fun getDisplayName(context: Context): String? {
        return securePrefs(context).getString(KEY_DISPLAY_NAME, null)
    }

    fun setDisplayName(context: Context, name: String?) {
        securePrefs(context).edit().apply {
            if (name.isNullOrBlank()) {
                remove(KEY_DISPLAY_NAME)
            } else {
                putString(KEY_DISPLAY_NAME, name.trim())
            }
        }.apply()
    }

    private fun isStrongBoxSupported(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
    }

    private fun securePrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
}
