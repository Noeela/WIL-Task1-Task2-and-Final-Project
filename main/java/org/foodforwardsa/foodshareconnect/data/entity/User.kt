package org.foodforwardsa.foodshareconnect.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import org.foodforwardsa.foodshareconnect.data.UserRole

/**
 * Local account record. Passwords are never stored in plain text - only a
 * PBKDF2 hash + a per-user random salt (see [org.foodforwardsa.foodshareconnect.util.PasswordHasher]),
 * mirroring how ASP.NET Core Identity secures the web version of this system.
 */
@Entity(
    tableName = "users",
    indices = [Index(value = ["email"], unique = true)],
    foreignKeys = [
        ForeignKey(
            entity = BeneficiaryOrganisation::class,
            parentColumns = ["id"],
            childColumns = ["beneficiaryOrganisationId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class User(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val email: String,
    val passwordHash: String,
    val passwordSalt: String,
    val displayName: String,
    val role: UserRole,
    val beneficiaryOrganisationId: Long? = null,

    // Two-factor authentication (TOTP), required for Admin accounts.
    val twoFactorEnabled: Boolean = false,
    val twoFactorSecret: String? = null,

    // Basic brute-force protection, mirroring ASP.NET Identity's lockout policy.
    val failedLoginAttempts: Int = 0,
    val lockedUntilEpochMillis: Long? = null
)
