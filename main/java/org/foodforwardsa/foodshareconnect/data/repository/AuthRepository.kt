package org.foodforwardsa.foodshareconnect.data.repository

import org.foodforwardsa.foodshareconnect.data.AppDatabase
import org.foodforwardsa.foodshareconnect.data.UserRole
import org.foodforwardsa.foodshareconnect.data.VettingStatus
import org.foodforwardsa.foodshareconnect.data.entity.BeneficiaryOrganisation
import org.foodforwardsa.foodshareconnect.data.entity.User
import org.foodforwardsa.foodshareconnect.util.OperationResult
import org.foodforwardsa.foodshareconnect.util.PasswordHasher
import org.foodforwardsa.foodshareconnect.util.TotpUtil
import org.foodforwardsa.foodshareconnect.util.safeCall

/** What the UI needs to decide what screen comes next after a login attempt. */
sealed class LoginOutcome {
    data class Success(val user: User) : LoginOutcome()
    object RequiresTwoFactor : LoginOutcome()
    data class Rejected(val reason: String) : LoginOutcome()
}

class AuthRepository(private val db: AppDatabase) {

    private val userDao = db.userDao()
    private val orgDao = db.beneficiaryOrganisationDao()

    companion object {
        private const val MAX_FAILED_ATTEMPTS = 5
        private const val LOCKOUT_MILLIS = 10 * 60 * 1000L // 10 minutes, mirrors the web app's Identity lockout policy
    }

    data class RegistrationInput(
        val organisationName: String,
        val contactPersonName: String,
        val email: String,
        val phoneNumber: String,
        val physicalAddress: String,
        val province: String,
        val npoRegistrationNumber: String,
        val estimatedBeneficiaries: Int,
        val password: String
    )

    suspend fun register(input: RegistrationInput): OperationResult<Unit> = safeCall("Could not complete registration") {
        val existingUser = userDao.findByEmail(input.email)
        if (existingUser != null) {
            throw IllegalStateException("An account already exists for this email address.")
        }

        val duplicateOrgCount = orgDao.countDuplicates(input.email, input.npoRegistrationNumber)
        if (duplicateOrgCount > 0) {
            throw IllegalStateException("An organisation with this email or NPO registration number is already registered.")
        }

        val org = BeneficiaryOrganisation(
            organisationName = input.organisationName,
            contactPersonName = input.contactPersonName,
            email = input.email,
            phoneNumber = input.phoneNumber,
            physicalAddress = input.physicalAddress,
            province = input.province,
            npoRegistrationNumber = input.npoRegistrationNumber,
            estimatedBeneficiaries = input.estimatedBeneficiaries,
            vettingStatus = VettingStatus.PENDING_REVIEW
        )
        val orgId = orgDao.insert(org)

        val (hash, salt) = PasswordHasher.hash(input.password)
        userDao.insert(
            User(
                email = input.email,
                passwordHash = hash,
                passwordSalt = salt,
                displayName = input.contactPersonName,
                role = UserRole.BENEFICIARY_ORG,
                beneficiaryOrganisationId = orgId
            )
        )
    }

    /** Step 1: validate email + password. Admin accounts stop here and require [verifyTwoFactor]. */
    suspend fun login(email: String, password: String): LoginOutcome {
        val user = userDao.findByEmail(email) ?: return LoginOutcome.Rejected("Invalid email or password.")

        val lockedUntil = user.lockedUntilEpochMillis
        if (lockedUntil != null && lockedUntil > System.currentTimeMillis()) {
            return LoginOutcome.Rejected("This account is temporarily locked due to repeated failed sign-in attempts. Please try again later.")
        }

        val passwordValid = PasswordHasher.verify(password, user.passwordHash, user.passwordSalt)
        if (!passwordValid) {
            val attempts = user.failedLoginAttempts + 1
            val lockUntil = if (attempts >= MAX_FAILED_ATTEMPTS) System.currentTimeMillis() + LOCKOUT_MILLIS else null
            userDao.update(user.copy(failedLoginAttempts = attempts, lockedUntilEpochMillis = lockUntil))
            return LoginOutcome.Rejected("Invalid email or password.")
        }

        // Successful password check resets the failed-attempt counter.
        if (user.failedLoginAttempts != 0 || user.lockedUntilEpochMillis != null) {
            userDao.update(user.copy(failedLoginAttempts = 0, lockedUntilEpochMillis = null))
        }

        return if (user.role == UserRole.ADMIN && user.twoFactorEnabled) {
            LoginOutcome.RequiresTwoFactor
        } else {
            LoginOutcome.Success(user)
        }
    }

    suspend fun verifyTwoFactor(email: String, code: String): LoginOutcome {
        val user = userDao.findByEmail(email) ?: return LoginOutcome.Rejected("Session expired. Please sign in again.")
        val secret = user.twoFactorSecret ?: return LoginOutcome.Rejected("Two-factor authentication is not set up correctly on this account.")

        return if (TotpUtil.verifyCode(secret, code)) {
            LoginOutcome.Success(user)
        } else {
            val attempts = user.failedLoginAttempts + 1
            val lockUntil = if (attempts >= MAX_FAILED_ATTEMPTS) System.currentTimeMillis() + LOCKOUT_MILLIS else null
            userDao.update(user.copy(failedLoginAttempts = attempts, lockedUntilEpochMillis = lockUntil))
            LoginOutcome.Rejected("Invalid authenticator code. Please try again.")
        }
    }

    suspend fun beginTwoFactorEnrolment(userId: Long): OperationResult<Pair<String, String>> =
        safeCall("Could not start two-factor setup") {
            val user = userDao.findById(userId) ?: throw IllegalStateException("Account not found.")
            val secret = TotpUtil.generateSecret()
            userDao.update(user.copy(twoFactorSecret = secret, twoFactorEnabled = false))
            val uri = TotpUtil.buildAuthenticatorUri(secret, user.email)
            secret to uri
        }

    suspend fun confirmTwoFactorEnrolment(userId: Long, code: String): OperationResult<Unit> =
        safeCall("Could not verify code") {
            val user = userDao.findById(userId) ?: throw IllegalStateException("Account not found.")
            val secret = user.twoFactorSecret ?: throw IllegalStateException("Start two-factor setup again.")
            if (!TotpUtil.verifyCode(secret, code)) {
                throw IllegalArgumentException("Verification code is incorrect.")
            }
            userDao.update(user.copy(twoFactorEnabled = true))
        }
}
