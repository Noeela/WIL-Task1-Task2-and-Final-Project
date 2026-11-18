package org.foodforwardsa.foodshareconnect.util

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Salted PBKDF2 password hashing (100,000 iterations, SHA-256, 256-bit key),
 * the on-device equivalent of the salted hashing ASP.NET Core Identity
 * performs on the web version of this system. Plaintext passwords are never
 * written to the database.
 */
object PasswordHasher {

    private const val ITERATIONS = 100_000
    private const val KEY_LENGTH = 256

    /** Returns (hash, salt), both Base64-encoded, ready to store in the User entity. */
    fun hash(password: String, saltBase64: String? = null): Pair<String, String> {
        val salt = if (saltBase64 != null) {
            Base64.decode(saltBase64, Base64.NO_WRAP)
        } else {
            ByteArray(16).also { SecureRandom().nextBytes(it) }
        }

        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hashBytes = factory.generateSecret(spec).encoded

        return Base64.encodeToString(hashBytes, Base64.NO_WRAP) to Base64.encodeToString(salt, Base64.NO_WRAP)
    }

    fun verify(password: String, expectedHash: String, saltBase64: String): Boolean {
        val (computedHash, _) = hash(password, saltBase64)
        // Constant-time-ish comparison to reduce timing side-channel risk.
        return computedHash.length == expectedHash.length &&
            computedHash.zip(expectedHash).fold(true) { acc, (a, b) -> acc and (a == b) }
    }
}
