package cz.hcasc.hotel.security

import android.util.Base64

/**
 * Simplified challenge signer – returns the nonce as-is (placeholder).
 */
object ChallengeSigner {
    fun signNonceBase64(nonceB64: String): String {
        // In a full implementation, this would sign using the keystore private key.
        return nonceB64
    }
}
