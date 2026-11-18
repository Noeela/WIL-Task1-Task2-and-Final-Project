package org.foodforwardsa.foodshareconnect.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.foodforwardsa.foodshareconnect.data.AppDatabase
import org.foodforwardsa.foodshareconnect.data.repository.AuthRepository
import org.foodforwardsa.foodshareconnect.data.repository.LoginOutcome
import org.foodforwardsa.foodshareconnect.databinding.ActivityTwoFactorBinding
import org.foodforwardsa.foodshareconnect.session.SessionManager
import org.foodforwardsa.foodshareconnect.ui.admin.AdminDashboardActivity

class TwoFactorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTwoFactorBinding
    private lateinit var authRepository: AuthRepository
    private lateinit var email: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTwoFactorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authRepository = AuthRepository(AppDatabase.getInstance(this))
        email = intent.getStringExtra(EXTRA_EMAIL) ?: run { finish(); return }

        binding.buttonVerify.setOnClickListener { attemptVerify() }
    }

    private fun attemptVerify() {
        val code = binding.inputCode.text?.toString()?.trim().orEmpty()
        if (code.length != 6 || code.any { !it.isDigit() }) {
            showError("Enter the 6-digit code shown in your authenticator app.")
            return
        }

        setLoading(true)
        lifecycleScope.launch {
            try {
                when (val outcome = authRepository.verifyTwoFactor(email, code)) {
                    is LoginOutcome.Success -> {
                        val user = outcome.user
                        SessionManager(this@TwoFactorActivity)
                            .startSession(user.id, user.displayName, user.role, user.beneficiaryOrganisationId)
                        startActivity(Intent(this@TwoFactorActivity, AdminDashboardActivity::class.java))
                        finish()
                    }
                    is LoginOutcome.Rejected -> showError(outcome.reason)
                    is LoginOutcome.RequiresTwoFactor -> showError("Unexpected state. Please sign in again.")
                }
            } catch (e: Exception) {
                showError("An unexpected error occurred. Please try again.")
            } finally {
                setLoading(false)
            }
        }
    }

    private fun showError(message: String) {
        binding.textError.text = message
        binding.textError.visibility = android.view.View.VISIBLE
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) android.view.View.VISIBLE else android.view.View.GONE
        binding.buttonVerify.isEnabled = !loading
    }

    companion object {
        const val EXTRA_EMAIL = "extra_email"
    }
}
