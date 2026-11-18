package org.foodforwardsa.foodshareconnect.ui.auth

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.foodforwardsa.foodshareconnect.data.AppDatabase
import org.foodforwardsa.foodshareconnect.data.repository.AuthRepository
import org.foodforwardsa.foodshareconnect.databinding.ActivityRegisterBinding
import org.foodforwardsa.foodshareconnect.util.OperationResult
import org.foodforwardsa.foodshareconnect.util.ValidationUtils

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var authRepository: AuthRepository

    private val provinces = listOf(
        "Eastern Cape", "Free State", "Gauteng", "KwaZulu-Natal", "Limpopo",
        "Mpumalanga", "Northern Cape", "North West", "Western Cape"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val toolbar = binding.toolbar.root as com.google.android.material.appbar.MaterialToolbar
        setSupportActionBar(toolbar)
        toolbar.title = "Register Organisation"
        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
        toolbar.setNavigationOnClickListener { finish() }

        binding.spinnerProvince.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, provinces)

        authRepository = AuthRepository(AppDatabase.getInstance(this))
        binding.buttonSubmit.setOnClickListener { attemptSubmit() }
    }

    private fun attemptSubmit() {
        hideError()

        val orgName = binding.inputOrgName.text?.toString()?.trim().orEmpty()
        val contactName = binding.inputContactName.text?.toString()?.trim().orEmpty()
        val email = binding.inputEmail.text?.toString()?.trim().orEmpty()
        val phone = binding.inputPhone.text?.toString()?.trim().orEmpty()
        val address = binding.inputAddress.text?.toString()?.trim().orEmpty()
        val province = binding.spinnerProvince.selectedItem as? String
        val npoRegNo = binding.inputNpoRegNo.text?.toString()?.trim().orEmpty()
        val beneficiariesText = binding.inputBeneficiaries.text?.toString()?.trim().orEmpty()
        val password = binding.inputPassword.text?.toString().orEmpty()
        val confirmPassword = binding.inputConfirmPassword.text?.toString().orEmpty()

        // Full client-side validation, mirroring the constraints enforced again
        // inside AuthRepository.register() - so nothing invalid ever crashes a
        // downstream calculation and the user gets an immediate, specific message.
        if (orgName.length < 2) { showError("Please enter your organisation's full name."); return }
        if (contactName.isBlank()) { showError("Please enter a contact person's name."); return }
        if (!ValidationUtils.isValidEmail(email)) { showError("Please enter a valid email address."); return }
        if (!ValidationUtils.isValidSaPhoneNumber(phone)) { showError("Please enter a valid South African phone number, e.g. 0821234567."); return }
        if (address.isBlank()) { showError("Please enter your organisation's physical address."); return }
        if (province == null) { showError("Please select a province."); return }
        if (npoRegNo.isBlank()) { showError("Please enter your NPO registration number."); return }
        val beneficiaries = ValidationUtils.parsePositiveInt(beneficiariesText, 1_000_000)
            ?: run { showError("Please enter a valid number of beneficiaries (a whole number greater than zero)."); return }
        if (!ValidationUtils.isValidPassword(password)) { showError("Password must be at least 8 characters long."); return }
        if (password != confirmPassword) { showError("Passwords do not match."); return }

        setLoading(true)
        lifecycleScope.launch {
            try {
                val result = authRepository.register(
                    AuthRepository.RegistrationInput(
                        organisationName = orgName,
                        contactPersonName = contactName,
                        email = email,
                        phoneNumber = phone,
                        physicalAddress = address,
                        province = province,
                        npoRegistrationNumber = npoRegNo,
                        estimatedBeneficiaries = beneficiaries,
                        password = password
                    )
                )
                when (result) {
                    is OperationResult.Success -> {
                        Toast.makeText(
                            this@RegisterActivity,
                            "Registration submitted. Your organisation is pending review by FoodForward SA.",
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    }
                    is OperationResult.Failure -> showError(result.message)
                }
            } catch (e: Exception) {
                showError("An unexpected error occurred while submitting your registration. Please try again.")
            } finally {
                setLoading(false)
            }
        }
    }

    private fun showError(message: String) {
        binding.textError.text = message
        binding.textError.visibility = View.VISIBLE
    }

    private fun hideError() {
        binding.textError.visibility = View.GONE
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.buttonSubmit.isEnabled = !loading
    }
}
