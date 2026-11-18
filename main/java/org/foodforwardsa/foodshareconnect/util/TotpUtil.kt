package org.foodforwardsa.foodshareconnect.util

import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.pow

/**
 * Minimal RFC 6238 Time-based One-Time Password implementation using
 * standard Java crypto (HmacSHA1), with no third-party dependency. Produces
 * and verifies 6-digit codes compatible with Google Authenticator, Microsoft
 * Authenticator, etc. - the same mechanism used for Admin 2FA on the web app.
 */
object TotpUtil {

    private const val TIME_STEP_SECONDS = 30L
    private const val CODE_DIGITS = 6
    private val BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

    fun generateSecret(): String {
        val bytes = ByteArray(20)
        SecureRandom().nextBytes(bytes)
        return base32Encode(bytes)
    }

    fun buildAuthenticatorUri(secret: String, email: String): String {
        val issuer = "FoodShareConnect"
        return "otpauth://totp/$issuer:$email?secret=$secret&issuer=$issuer&digits=$CODE_DIGITS&period=$TIME_STEP_SECONDS"
    }

    /** Validates [code] against [secret], tolerating +/- 1 time step of clock drift. */
    fun verifyCode(secret: String, code: String, timeMillis: Long = System.currentTimeMillis()): Boolean {
        val cleanCode = code.trim().replace(" ", "")
        if (cleanCode.length != CODE_DIGITS || cleanCode.any { !it.isDigit() }) return false

        val currentStep = timeMillis / 1000 / TIME_STEP_SECONDS
        for (drift in -1..1) {
            if (generateCode(secret, currentStep + drift) == cleanCode) return true
        }
        return false
    }

    private fun generateCode(secret: String, timeStep: Long): String {
        val key = base32Decode(secret)
        val data = ByteArray(8)
        var value = timeStep
        for (i in 7 downTo 0) {
            data[i] = (value and 0xff).toByte()
            value = value shr 8
        }

        val mac = Mac.getInstance("HmacSHA1")
        mac.init(SecretKeySpec(key, "HmacSHA1"))
        val hash = mac.doFinal(data)

        val offset = (hash[hash.size - 1].toInt() and 0xf)
        val binary = ((hash[offset].toInt() and 0x7f) shl 24) or
            ((hash[offset + 1].toInt() and 0xff) shl 16) or
            ((hash[offset + 2].toInt() and 0xff) shl 8) or
            (hash[offset + 3].toInt() and 0xff)

        val otp = binary % 10.0.pow(CODE_DIGITS).toInt()
        return otp.toString().padStart(CODE_DIGITS, '0')
    }

    private fun base32Encode(data: ByteArray): String {
        val sb = StringBuilder()
        var bits = 0
        var value = 0
        for (b in data) {
            value = (value shl 8) or (b.toInt() and 0xff)
            bits += 8
            while (bits >= 5) {
                sb.append(BASE32_ALPHABET[(value shr (bits - 5)) and 0x1f])
                bits -= 5
            }
        }
        if (bits > 0) {
            sb.append(BASE32_ALPHABET[(value shl (5 - bits)) and 0x1f])
        }
        return sb.toString()
    }

    private fun base32Decode(encoded: String): ByteArray {
        val clean = encoded.trim().uppercase().replace("=", "")
        val output = ArrayList<Byte>()
        var bits = 0
        var value = 0
        for (c in clean) {
            val idx = BASE32_ALPHABET.indexOf(c)
            if (idx < 0) continue
            value = (value shl 5) or idx
            bits += 5
            if (bits >= 8) {
                output.add(((value shr (bits - 8)) and 0xff).toByte())
                bits -= 8
            }
        }
        return output.toByteArray()
    }
}
