package org.foodforwardsa.foodshareconnect.ui.auth

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.launch
import org.foodforwardsa.foodshareconnect.data.AppDatabase
import org.foodforwardsa.foodshareconnect.data.repository.AuthRepository
import org.foodforwardsa.foodshareconnect.databinding.ActivityEnableAuthenticatorBinding
import org.foodforwardsa.foodshareconnect.session.SessionManager
import org.foodforwardsa.foodshareconnect.util.OperationResult

class EnableAuthenticatorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEnableAuthenticatorBinding
    private lateinit var authRepository: AuthRepository
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEnableAuthenticatorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val toolbar = binding.toolbar.root as MaterialToolbar
        setSupportActionBar(toolbar)
        toolbar.title = "Two-Factor Setup"
        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
        toolbar.setNavigationOnClickListener { finish() }

        authRepository = AuthRepository(AppDatabase.getInstance(this))
        session = SessionManager(this)

        beginEnrolment()
        binding.buttonVerify.setOnClickListener { attemptConfirm() }
    }

    private fun beginEnrolment() {
        lifecycleScope.launch {
            when (val result = authRepository.beginTwoFactorEnrolment(session.userId)) {
                is OperationResult.Success -> {
                    val (secret, _) = result.data
                    binding.textSecretKey.text = secret.chunked(4).joinToString(" ")
                }
                is OperationResult.Failure -> {
                    showError(result.message)
                    binding.buttonVerify.isEnabled = false
                }
            }
        }
    }

    private fun attemptConfirm() {
        val code = binding.inputCode.text?.toString()?.trim().orEmpty()
        if (code.length != 6 || code.any { !it.isDigit() }) {
            showError("Enter the 6-digit code shown in your authenticator app.")
            return
        }

        setLoading(true)
        lifecycleScope.launch {
            try {
                when (val result = authRepository.confirmTwoFactorEnrolment(session.userId, code)) {
                    is OperationResult.Success -> {
                        Toast.makeText(
                            this@EnableAuthenticatorActivity,
                            "Two-factor authentication has been enabled on your account.",
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    }
                    is OperationResult.Failure -> showError(result.message)
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
        binding.textError.visibility = View.VISIBLE
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.buttonVerify.isEnabled = !loading
    }
}
