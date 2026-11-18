package org.foodforwardsa.foodshareconnect.util

import android.util.Patterns
import java.util.regex.Pattern

/**
 * Central place for all input-validation rules, so every screen enforces the
 * same constraints. Called before any database write, in addition to
 * Room's own column constraints - this is the app's client-side half of the
 * "error handling for all input fields" requirement.
 */
object ValidationUtils {

    private val SA_PHONE_PATTERN = Pattern.compile("^(\\+27|0)[6-8][0-9]{8}$")

    fun isValidEmail(email: String): Boolean =
        email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()

    fun isValidSaPhoneNumber(phone: String): Boolean =
        SA_PHONE_PATTERN.matcher(phone.trim()).matches()

    fun isValidPassword(password: String): Boolean = password.length >= 8

    fun isNotBlank(value: String?): Boolean = !value.isNullOrBlank()

    fun parsePositiveDouble(value: String, max: Double): Double? {
        val d = value.trim().toDoubleOrNull() ?: return null
        return if (d > 0 && d <= max) d else null
    }

    fun parsePositiveInt(value: String, max: Int): Int? {
        val i = value.trim().toIntOrNull() ?: return null
        return if (i > 0 && i <= max) i else null
    }
}
