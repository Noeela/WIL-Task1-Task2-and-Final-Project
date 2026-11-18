package org.foodforwardsa.foodshareconnect.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.foodforwardsa.foodshareconnect.data.AppDatabase
import org.foodforwardsa.foodshareconnect.data.UserRole
import org.foodforwardsa.foodshareconnect.data.repository.AuthRepository
import org.foodforwardsa.foodshareconnect.data.repository.LoginOutcome
import org.foodforwardsa.foodshareconnect.databinding.ActivityLoginBinding
import org.foodforwardsa.foodshareconnect.session.SessionManager
import org.foodforwardsa.foodshareconnect.ui.admin.AdminDashboardActivity
import org.foodforwardsa.foodshareconnect.ui.beneficiary.BeneficiaryDashboardActivity
import org.foodforwardsa.foodshareconnect.ui.contact.ContactUsActivity
import org.foodforwardsa.foodshareconnect.util.ValidationUtils

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var authRepository: AuthRepository
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authRepository = AuthRepository(AppDatabase.getInstance(this))
        session = SessionManager(this)

        binding.buttonSignIn.setOnClickListener { attemptLogin() }
        binding.buttonRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        binding.textContactUs.setOnClickListener {
            startActivity(Intent(this, ContactUsActivity::class.java))
        }
    }

    private fun attemptLogin() {
        hideError()
        val email = binding.inputEmail.text?.toString()?.trim().orEmpty()
        val password = binding.inputPassword.text?.toString().orEmpty()

        if (!ValidationUtils.isValidEmail(email)) {
            showError("Please enter a valid email address."); return
        }
        if (password.isBlank()) {
            showError("Please enter your password."); return
        }

        setLoading(true)
        lifecycleScope.launch {
            try {
                when (val outcome = authRepository.login(email, password)) {
                    is LoginOutcome.Success -> {
                        val user = outcome.user
                        session.startSession(user.id, user.displayName, user.role, user.beneficiaryOrganisationId)
                        val target = if (user.role == UserRole.ADMIN) AdminDashboardActivity::class.java
                                     else BeneficiaryDashboardActivity::class.java
                        startActivity(Intent(this@LoginActivity, target))
                        finish()
                    }
                    is LoginOutcome.RequiresTwoFactor -> {
                        startActivity(Intent(this@LoginActivity, TwoFactorActivity::class.java).apply {
                            putExtra(TwoFactorActivity.EXTRA_EMAIL, email)
                        })
                    }
                    is LoginOutcome.Rejected -> showError(outcome.reason)
                }
            } catch (e: Exception) {
                showError("An unexpected error occurred while signing you in. Please try again.")
            } finally {
                setLoading(false)
            }
        }
    }

    private fun showError(message: String) {
        binding.textError.text = message
        binding.textError.visibility = android.view.View.VISIBLE
    }

    private fun hideError() {
        binding.textError.visibility = android.view.View.GONE
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) android.view.View.VISIBLE else android.view.View.GONE
        binding.buttonSignIn.isEnabled = !loading
        binding.buttonRegister.isEnabled = !loading
    }
}
