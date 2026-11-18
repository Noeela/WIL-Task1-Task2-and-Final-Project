package org.foodforwardsa.foodshareconnect.session

import android.content.Context
import androidx.core.content.edit
import org.foodforwardsa.foodshareconnect.data.UserRole

/** Lightweight session store - keeps the signed-in user's id/role/name between screens and app restarts. */
class SessionManager(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("foodshareconnect_session", Context.MODE_PRIVATE)

    var userId: Long
        get() = prefs.getLong(KEY_USER_ID, -1L)
        private set(value) = prefs.edit { putLong(KEY_USER_ID, value) }

    var displayName: String
        get() = prefs.getString(KEY_DISPLAY_NAME, "") ?: ""
        private set(value) = prefs.edit { putString(KEY_DISPLAY_NAME, value) }

    var role: UserRole?
        get() = prefs.getString(KEY_ROLE, null)?.let { UserRole.valueOf(it) }
        private set(value) = prefs.edit { putString(KEY_ROLE, value?.name) }

    var beneficiaryOrgId: Long?
        get() = prefs.getLong(KEY_ORG_ID, -1L).takeIf { it != -1L }
        private set(value) = prefs.edit { putLong(KEY_ORG_ID, value ?: -1L) }

    val isLoggedIn: Boolean get() = userId != -1L

    fun startSession(userId: Long, displayName: String, role: UserRole, beneficiaryOrgId: Long?) {
        this.userId = userId
        this.displayName = displayName
        this.role = role
        this.beneficiaryOrgId = beneficiaryOrgId
    }

    fun clear() {
        prefs.edit { clear() }
    }

    private companion object {
        const val KEY_USER_ID = "user_id"
        const val KEY_DISPLAY_NAME = "display_name"
        const val KEY_ROLE = "role"
        const val KEY_ORG_ID = "org_id"
    }
}
